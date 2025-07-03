package io.openliberty.http.netty.timeout;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.netty.NettyHttpChannelConfig;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.openliberty.http.options.HttpOption;

public class H2TimeoutHandler extends ChannelDuplexHandler{

    private final int idleSeconds;

    public H2TimeoutHandler(NettyHttpChannelConfig config){
        Object idleMillis = (long) config.get(HttpOption.HTTP2_CONNECTION_IDLE_TIMEOUT);
        this.idleSeconds = (int)TimeUnit.MILLISECONDS.toSeconds((long)idleMillis);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context){
        context.pipeline().addFirst("h2TimeoutHandler", new IdleStateHandler(0, 0, idleSeconds));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception{
        if(event instanceof IdleStateEvent){
            IdleStateEvent e = (IdleStateEvent) event;
            if(e.state() == IdleState.ALL_IDLE){
                Http2ConnectionHandler h2 = context.pipeline().get(Http2ConnectionHandler.class);
                if (h2 != null) {
                    Http2Connection connection = h2.connection();
                    int lastStream = connection.remote().lastStreamCreated();
                    h2.goAway(context, lastStream, Http2Error.NO_ERROR.code(), Unpooled.EMPTY_BUFFER, context.newPromise());
                }
                context.close();
                return;
            } 
        }
        super.userEventTriggered(context, event);
    }
    
}
