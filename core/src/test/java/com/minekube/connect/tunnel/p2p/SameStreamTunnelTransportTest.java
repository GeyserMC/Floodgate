package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.minekube.connect.tunnel.TunnelConn;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SameStreamTunnelTransportTest {

    @Test
    void sendsAcceptedWhenTunnelOpensAndUsesSameStream() {
        Stream stream = mock(Stream.class);
        List<String> accepted = new ArrayList<>();
        SameStreamTunnelTransport transport = new SameStreamTunnelTransport(stream, accepted::add);
        RecordingHandler handler = new RecordingHandler();

        TunnelConn conn = transport.tunnel("", "session-1", handler);
        conn.write(new byte[] {1, 2, 3});

        assertTrue(conn.opened());
        assertArrayEquals(new String[] {"session-1"}, accepted.toArray(String[]::new));
        verify(stream).pushHandler(any(ChannelHandler.class));
        ArgumentCaptor<Object> outbound = ArgumentCaptor.forClass(Object.class);
        verify(stream).writeAndFlush(outbound.capture());
        ByteBuf buf = (ByteBuf) outbound.getValue();
        assertArrayEquals(new byte[] {1, 2, 3}, ByteBufUtil.getBytes(buf));
    }

    @Test
    void forwardsInboundBytesToTunnelHandler() {
        Stream stream = mock(Stream.class);
        SameStreamTunnelTransport transport = new SameStreamTunnelTransport(stream, ignored -> {});
        RecordingHandler handler = new RecordingHandler();
        transport.tunnel("", "session-1", handler);

        ArgumentCaptor<ChannelHandler> captor = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(stream).pushHandler(captor.capture());
        EmbeddedChannel channel = new EmbeddedChannel(captor.getValue());
        channel.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(new byte[] {4, 5}));

        assertArrayEquals(new byte[] {4, 5}, handler.received);
    }

    @Test
    void reportsLibp2pTransportType() {
        SameStreamTunnelTransport transport = new SameStreamTunnelTransport(mock(Stream.class), ignored -> {});

        org.junit.jupiter.api.Assertions.assertEquals(Type.TYPE_LIBP2P, transport.type());
    }

    private static final class RecordingHandler implements TunnelConn.Handler {
        private byte[] received;

        @Override
        public void onReceive(byte[] data) {
            received = data;
        }

        @Override
        public void onError(Throwable t) {
        }
    }
}
