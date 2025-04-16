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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.timeout.IdleStateHandler;

public class TimeoutHandler extends ChannelDuplexHandler{

    private static final String REQUEST_IDLE_NAME  = "requestIdleHandler";
    private static final String PERSIST_IDLE_NAME  = "persistIdleHandler";
    private static final String REQUEST_IDLE_EVENT = "requestIdleEventHandler";
    private static final String PERSIST_IDLE_EVENT = "persistIdleEventHandler";

    private final int requestReadTimeoutSeconds;
    private final int persistReadTimeoutSeconds;

    public TimeoutHandler(NettyHttpChannelConfig config) {
        this.requestReadTimeoutSeconds = config.getReadTimeout();   
        this.persistReadTimeoutSeconds = config.getPersistTimeout(); 
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        activateRequestIdle(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            // We have begun reading a new request
            activateRequestIdle(ctx);
        }
        super.channelRead(ctx, msg);
    }

    /**
     * Capture outbound writes. Once the server writes a FullHttpResponse,
     * we assume we've finished handling the request. At that point,
     * let's switch to "persist" mode
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpResponse) {
            promise.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    activatePersistIdle(ctx);
                } else {
                    // TODO -> Verify if anything needed. Should be handled by tcp write handler
                }
            });
        }
        super.write(ctx, msg, promise);
    }

    /**
     * Add the 'request read' IdleStateHandler if not present;
     * remove the 'persist' idle if it's currently in place.
     */
    private void activateRequestIdle(ChannelHandlerContext ctx) {
        removeIfExists(ctx, PERSIST_IDLE_NAME, PERSIST_IDLE_EVENT);
        if (!handlerExists(ctx, REQUEST_IDLE_NAME) && requestReadTimeoutSeconds > 0) {
            IdleStateHandler idle = new IdleStateHandler(requestReadTimeoutSeconds, 0, 0);
            ctx.pipeline().addBefore(ctx.name(), REQUEST_IDLE_NAME, idle);
            ctx.pipeline().addAfter(REQUEST_IDLE_NAME, REQUEST_IDLE_EVENT,
                new TimeoutEventHandler(TimeoutType.READ, requestReadTimeoutSeconds));
        }
    }

    /**
     * Add the 'persist keep-alive' IdleStateHandler if not present;
     * remove the 'request' idle if currently in place.
     */
    private void activatePersistIdle(ChannelHandlerContext ctx) {
        removeIfExists(ctx, REQUEST_IDLE_NAME, REQUEST_IDLE_EVENT);
        if (!handlerExists(ctx, PERSIST_IDLE_NAME) && persistReadTimeoutSeconds > 0) {
            IdleStateHandler idle = new IdleStateHandler(persistReadTimeoutSeconds, 0, 0);
            ctx.pipeline().addBefore(ctx.name(), PERSIST_IDLE_NAME, idle);
            ctx.pipeline().addAfter(PERSIST_IDLE_NAME, PERSIST_IDLE_EVENT,
                new TimeoutEventHandler(TimeoutType.PERSIST, persistReadTimeoutSeconds));
        }
    }

    private void removeIfExists(ChannelHandlerContext ctx, String handlerName, String eventHandlerName) {
        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
        if (pipeline.get(eventHandlerName) != null) {
            pipeline.remove(eventHandlerName);
        }
    }

    private boolean handlerExists(ChannelHandlerContext ctx, String name) {
        return ctx.pipeline().get(name) != null;
    }
}
