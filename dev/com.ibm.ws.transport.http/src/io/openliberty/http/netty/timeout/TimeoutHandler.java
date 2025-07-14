/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout;

import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.openliberty.http.netty.timeout.exception.H2IdleTimeoutException;
import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;
import io.openliberty.http.options.TcpOption;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.ScheduledFuture;

public class TimeoutHandler extends ChannelDuplexHandler{


    private static final TimeUnit LEGACY_UNIT = TimeUnit.MILLISECONDS;

    private final int readTimeout;
    private final int persistTimeout;
    private final int inactivityTimeout;
    private final int h2InactivityTimeout;

    private final boolean useKeepAlive;
    private volatile boolean keepAliveRequested;

    private volatile TimeoutType phase = TimeoutType.READ;
    private volatile ChannelHandlerContext context;

    private final static int USE_TCP_TIMEOUT = 0;
    private volatile boolean readRetried;

    private final AtomicReference<ScheduledFuture<?>> currentTimeout = new AtomicReference<>();

    private final Runnable timerTask = () -> timeoutFired();

    public TimeoutHandler(NettyHttpChannelConfig config) {
        
        this.inactivityTimeout = (int) config.get(TcpOption.INACTIVITY_TIMEOUT);
        this.readTimeout = initTimeout(config.getReadTimeout());
        this.persistTimeout = initTimeout(config.getPersistTimeout());
        this.h2InactivityTimeout = initTimeout(Math.toIntExact(config.getH2ConnCloseTimeout()*1000)); // this might be in seconds, and need converting
        this.useKeepAlive = config.isKeepAliveEnabled();

        System.out.printf("Timeouthandler ctor - read=%d ms, persist=%d ms, idle=%d ms, h2Idle=%d ms, keepAlive=%b%n", readTimeout, persistTimeout, inactivityTimeout, h2InactivityTimeout, useKeepAlive);
    }

    private int initTimeout(int timeout){
        return timeout == USE_TCP_TIMEOUT ? inactivityTimeout : timeout;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        this.context = context;
        this.phase = TimeoutType.READ;
        activateTimer();
        super.channelActive(context);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        cancelTimer();
        super.channelInactive(context);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) throws Exception {
        cancelTimer();
        super.handlerRemoved(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            keepAliveRequested = HttpUtil.isKeepAlive((HttpMessage)message);
            System.out.println(">>> USE KEEPALIVE: " + keepAliveRequested);
            resetReadTimer();
        } else if(isH2(context)){
            resetReadTimer();
        }
        super.channelRead(context, message);
    }

    private void resetReadTimer(){
        System.out.printf("resetReadTImer - phase -> READ, keepAliveRequested=%b%n", keepAliveRequested);
        cancelTimer();
        phase = TimeoutType.READ;
        readRetried = false;
        activateTimer();
    }

    /**
     * Capture outbound writes. Once the server writes a FullHttpResponse,
     * we assume we've finished handling the request. At that point,
     * let's switch to "persist" mode
     * 
     * NOTE: Technically, persist should be for only the first read of the next request. 
     * But the way we currently operate, and with autoread enabled, requests are
     * aggregated and queued up prior to this point. So this only provides partial 
     * coverage of the legacy implementation. A more loyal implementation can be provided
     * by disabling auto-read which will be tackled at a later point. 
     */
    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        System.out.println(">>> WRITE and my protocol is: " + context.channel().attr(NettyHttpConstants.PROTOCOL).get());
        System.out.printf("write() – msg=%s, proto=%s, channelOpen=%b%n",
                          message.getClass().getSimpleName(),
                          context.channel().attr(NettyHttpConstants.PROTOCOL).get(),
                          context.channel().isOpen());

        System.out.println(">>> JUMP HERE: "+context.pipeline().names());
        

        boolean isWsHandshake = false;

        if (message instanceof HttpResponse) {
            HttpHeaders h = ((HttpResponse) message).headers();
            System.out.println("...headers: " + h.toString());
            isWsHandshake = h.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true) &&
                h.contains(HttpHeaderNames.UPGRADE, "websocket", true);
            keepAliveRequested = HttpUtil.isKeepAlive((HttpResponse) message);
            clearKeepAliveIfNeeded(context, (HttpResponse)message);
        }

        System.out.printf("   headers parsed = wsHS=%b, respKeepAlive=%b%n", isWsHandshake, keepAliveRequested);



        if(isH2(context) || message instanceof FullHttpResponse || message instanceof LastHttpContent){
            boolean armKeepAlive = keepAliveRequested && useKeepAlive && !isWsHandshake && !isWebSocket(context) && context.channel().isActive();

            System.out.printf("   [Timer] armPersist=%b (phase=%s) at %d ms%n", armKeepAlive, phase, (phase==TimeoutType.READ) ? readTimeout:persistTimeout);
            promise.addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()){
                    cancelTimer();
                    if(armKeepAlive){
                        phase = TimeoutType.PERSIST;
                        activateTimer();
                    }
                    
                }
            });
        }
        super.write(context, message, promise);
    }

    private void clearKeepAliveIfNeeded(ChannelHandlerContext ctx, HttpMessage msg) {
        // Any non-HTTP protocol (websocket / h2) ⇒ no keep-alive semantics
        NettyHttpConstants.ProtocolName proto = NettyHttpConstants.ProtocolName.from(ctx.channel().attr(NettyHttpConstants.PROTOCOL).get());
        if (proto != NettyHttpConstants.ProtocolName.HTTP1) {
            keepAliveRequested = false;
            return;
        }

        // Explicit Connection: close also kills keep-alive
        if (msg.headers().contains(HttpHeaderNames.CONNECTION,
                                   HttpHeaderValues.CLOSE, /* ignoreCase */ true)) {
            keepAliveRequested = false;
        }
    }

    private void activateTimer(){
        
        
        long timeout = isH2(context) ? h2InactivityTimeout 
            : (phase == TimeoutType.READ) ? readTimeout : persistTimeout;
        

        System.out.println(">>> Timeout is set to: " + timeout);
        if(context == null || timeout <= 0){
            return;
        }
        System.out.printf("activateTimer – phase=%s, h2=%b, timeout=%d ms%n",
                          phase, isH2(context), timeout);
        
        cancelTimer();
        ScheduledFuture<?> future = context.executor().schedule(timerTask, timeout, LEGACY_UNIT);
        currentTimeout.set(future);

    }

    private void timeoutFired(){
        System.out.printf("timeoutFired – phase=%s, readRetried=%b, h2=%b → firing %s%n",
                          phase, readRetried, isH2(context),
                          (isH2(context) ? "H2IdleTimeout" : (phase == TimeoutType.READ ? "ReadTimeout" : "PersistTimeout")));

        System.out.println(">>> looking to fire a timeout");
        IOException exception;
        if (phase == TimeoutType.READ && !readRetried) {
            readRetried = true;
            activateTimer();
            return;
        }

        if(isH2(context)){
            exception = new H2IdleTimeoutException(h2InactivityTimeout, LEGACY_UNIT);
        } else{
            exception = (phase == TimeoutType.READ) ? 
                    new ReadTimeoutException(readTimeout, LEGACY_UNIT) : new PersistTimeoutException(persistTimeout, LEGACY_UNIT);
        }
        System.out.println(">>> following exception to be thrown: "+exception);
        
        context.fireExceptionCaught(exception);
    }

    private void cancelTimer(){
        ScheduledFuture<?> current = currentTimeout.getAndSet(null);
        if(current != null){
            current.cancel(false); //Do not interrupt thread
        }
    }

    private static boolean isH2(ChannelHandlerContext context) {
        return NettyHttpConstants.ProtocolName.from(context.channel().attr(NettyHttpConstants.PROTOCOL).get()) == NettyHttpConstants.ProtocolName.HTTP2;
    }

    private static boolean isWebSocket(ChannelHandlerContext context){
        return NettyHttpConstants.ProtocolName.from(context.channel().attr(NettyHttpConstants.PROTOCOL).get()) == NettyHttpConstants.ProtocolName.WEBSOCKET;
    }
}
