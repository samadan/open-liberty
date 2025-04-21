package com.ibm.ws.netty.timeout;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.openliberty.http.netty.timeout.TimeoutEventHandler;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.http.netty.timeout.TimeoutType;
import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;
import io.openliberty.http.netty.timeout.exception.UnknownTimeoutException;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;

public class TimeoutHandlerTests {

    @Test
    public void readTimeoutExceptionMessageTest(){
        ReadTimeoutException exception = new ReadTimeoutException(5, TimeUnit.SECONDS);
        //TODO warning code -> assertEquals("", exception.getMessage());
        assertTrue(exception.getMessage().contains("5 seconds"));
    }

    @Test
    public void idleHandlerReadTimeout(){
        TimeoutEventHandler handler = new TimeoutEventHandler(TimeoutType.READ, 1, TimeUnit.SECONDS);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
        Throwable t = extractException(channel);

        assertNotNull("Handler should throw exception", t);
        assertTrue(t instanceof ReadTimeoutException);
        channel.close();
    }

    @Test
    public void readTimeoutHandlerOnActive(){
        NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
        when(config.getReadTimeout()).thenReturn(1000);
        when(config.getPersistTimeout()).thenReturn(1000);

        TimeoutHandler handler = new TimeoutHandler(config);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        assertNotNull("Request Idle handler is missing.", channel.pipeline().get("requestIdleHandler"));
    }

    @Test
    public void timeoutHandlerSwitchToPersist(){
        NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
        when(config.getReadTimeout()).thenReturn(2000);
        when(config.getPersistTimeout()).thenReturn(1000);
        TimeoutHandler handler = new TimeoutHandler(config);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        channel.writeOutbound(response);

        assertNotNull(channel.pipeline().get("persistIdleHandler"));

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
        Throwable t = extractException(channel);
        assertTrue(t instanceof PersistTimeoutException);
        channel.close();
    }


    private static Throwable extractException(EmbeddedChannel channel){
        try{
            channel.checkException();
            return null;

        }catch(Throwable t){
            return t;
        }
    }
}
