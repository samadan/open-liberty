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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;
import io.openliberty.http.netty.timeout.exception.TimeoutException;
import io.openliberty.http.netty.timeout.exception.UnknownTimeoutException;
import io.openliberty.http.netty.timeout.exception.WriteTimeoutException;

public class TimeoutEventHandler extends ChannelInboundHandlerAdapter{

    private final long timeout;
    private final TimeUnit unit;
    private final TimeoutType type;

    public TimeoutEventHandler(TimeoutType type, long timeout, TimeUnit unit) {
        this.type = type;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            TimeoutException exception;

            switch(type){
                case READ:      exception = new ReadTimeoutException(timeout, unit);    break;
                case WRITE:     exception = new WriteTimeoutException(timeout, unit);   break;
                case PERSIST:   exception = new PersistTimeoutException(timeout, unit); break;
                default:        exception = new UnknownTimeoutException(this.type);  // Future proof, should not happen unless enum expands
            }
            context.fireExceptionCaught(exception);
        } else {
            super.userEventTriggered(context, event);
        }
    }
}
