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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class TimeoutEventHandler extends ChannelInboundHandlerAdapter{

    //TODO log tr 

    private final TimeoutType type;
    private final int timeoutSeconds;

    public TimeoutEventHandler(TimeoutType type, int timeoutSeconds) {
        this.type = type;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvt = (IdleStateEvent) evt;
            System.err.println(type + " Idle Timeout after " 
                               + timeoutSeconds + "s => " + idleEvt.state() 
                               + " => closing channel.");
            //TODO -> Change this to handle the appropriate exception IOException extended read/write/persist
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
