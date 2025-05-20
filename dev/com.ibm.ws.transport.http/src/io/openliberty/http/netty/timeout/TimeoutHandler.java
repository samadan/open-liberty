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

import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.netty.NettyHttpChannelConfig;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateHandler;

// TODO -> Should this be @ChannelHandler.Sharable. Config is all equivalent across the endpoint
// so might be fine...
public class TimeoutHandler extends ChannelDuplexHandler{

    // IdleStateHandlers for request reads or persist reads (NETTY)
    private static final String NETTY_REQUEST_IDLE_HANDLER  = "requestIdleHandler";
    private static final String NETTY_PERSIST_IDLE_HANDLER  = "persistIdleHandler";

    // TimeoutEventHandlers for request read  or persist user event (OL)
    private static final String OL_REQUEST_IDLE_EVENT = "requestIdleEventHandler";
    private static final String OL_PERSIST_IDLE_EVENT = "persistIdleEventHandler";

    private static final TimeUnit LEGACY_UNIT = TimeUnit.MILLISECONDS;
    private static final TimeUnit PREFERRED_UNIT = TimeUnit.SECONDS;

    private final long configReadTimeout;
    private final long configPersistTimeout;

    public TimeoutHandler(NettyHttpChannelConfig config) {
        this.configReadTimeout = config.getReadTimeout();   
        this.configPersistTimeout = config.getPersistTimeout();
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        activateRead(context);
        super.channelActive(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            remove(context, NETTY_REQUEST_IDLE_HANDLER, OL_REQUEST_IDLE_EVENT);
            remove(context, NETTY_PERSIST_IDLE_HANDLER, OL_PERSIST_IDLE_EVENT);
        }
        super.channelRead(context, message);
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
        System.out.println(">>> Entered write >>>");
        promise.addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()){
                if(message instanceof FullHttpResponse || message instanceof LastHttpContent){
                    System.out.println(">>> Installing Persist timeout handler >>>");
                    activatePersist(context);
                }
            }
        });
        super.write(context, message, promise);
    }

    private void activateRead(ChannelHandlerContext context){
        swap(context, TimeoutType.READ, configReadTimeout, NETTY_REQUEST_IDLE_HANDLER, OL_REQUEST_IDLE_EVENT);
    }

    private void activatePersist(ChannelHandlerContext context){
        swap(context, TimeoutType.PERSIST, configPersistTimeout, NETTY_PERSIST_IDLE_HANDLER, OL_PERSIST_IDLE_EVENT);
    }


    private void swap(ChannelHandlerContext context, TimeoutType type, long timeout, String idleHandler, String eventHandler){
        System.out.println(">>> Swap timeout requested >>>");
        remove(context, NETTY_REQUEST_IDLE_HANDLER, OL_REQUEST_IDLE_EVENT);
        remove(context, NETTY_PERSIST_IDLE_HANDLER, OL_PERSIST_IDLE_EVENT);

        if(timeout <= 0) return;

        //TODO *1* - does creating new idleStateHandlers cause performance degradation? Check with perf team. If it does, 
        //we should try another strategy here like directly scheduling tasks. 
        IdleStateHandler idle = (type == TimeoutType.WRITE) ? 
                    new IdleStateHandler(0, timeout, 0, LEGACY_UNIT):
                    new IdleStateHandler(timeout, 0, 0, LEGACY_UNIT);

        context.pipeline().addBefore(context.name(), idleHandler, idle);
        long preferredDuration = asPreferred(timeout, LEGACY_UNIT);

        //TODO - same as TODO *1* -> perhaps we should consider persisting the same timeout event handler and clearing up the 
        //state. 
        context.pipeline().addAfter(idleHandler, eventHandler, new TimeoutEventHandler(type, preferredDuration, PREFERRED_UNIT));
    }

    /**
     * Used to remove the TimeoutEventHandler (ours) and/or IdleStateHandler (Netty) if 
     * they are configured in the pipeline. If the handlers are not in the pipeline, this
     * method results in a No-Op.
     * 
     * @param context
     * @param handlerName
     * @param eventName
     */
    private static void remove(ChannelHandlerContext context, String handlerName, String eventName){
        ChannelPipeline pipeline = context.pipeline();
        if(pipeline.get(handlerName) != null){
            System.out.println(">>> Removing " + handlerName +" >>>");
            pipeline.remove(handlerName);
        }
        if(pipeline.get(eventName) != null){
            System.out.println(">>> Removing " + eventName + " >>>");
            pipeline.remove(eventName);
        }
    }

    /**
     * Convert a timeout duration from any TimeUnit to our preferred
     * logging convention.
     * @param timeout
     * @param unit
     * @return
     */
    private static long asPreferred(long timeout, TimeUnit unit){

        return PREFERRED_UNIT.convert(timeout, unit);
    }
}
