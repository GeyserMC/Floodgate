package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionAccepted;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionAuthentication;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionGameProfile;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionOffer;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionPlayer;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class Libp2pSessionResponderTest {

    @Test
    void sendsAcceptedWhenLocalSessionOpensSameStreamTunnel() throws Exception {
        Stream stream = mock(Stream.class);
        AtomicReference<SessionProposal> proposalRef = new AtomicReference<>();
        Libp2pSessionResponder responder = new Libp2pSessionResponder((proposal, tunneler) -> {
            proposalRef.set(proposal);
            tunneler.tunnel(proposal.getSession(), new NoopHandler());
        });

        responder.handleOffer(stream, offer());

        assertEquals("session-1", proposalRef.get().getSession().getId());
        ArgumentCaptor<Object> outbound = ArgumentCaptor.forClass(Object.class);
        verify(stream).writeAndFlush(outbound.capture());
        SessionResponse response = P2PFrameCodec.read(
                new ByteBufInputStream((ByteBuf) outbound.getValue()),
                SessionResponse.parser(),
                P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
        assertTrue(response.hasAccepted(), response.toString());
        SessionAccepted accepted = response.getAccepted();
        assertTrue(accepted.getSameStreamData());
    }

    @Test
    void installedHandlerReadsOfferFrame() throws Exception {
        Stream stream = mock(Stream.class);
        AtomicReference<SessionProposal> proposalRef = new AtomicReference<>();
        Libp2pSessionResponder responder = new Libp2pSessionResponder((proposal, tunneler) -> {
            proposalRef.set(proposal);
            tunneler.tunnel(proposal.getSession(), new NoopHandler());
        });

        responder.install(stream);

        ArgumentCaptor<ChannelHandler> handler = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(stream, times(2)).pushHandler(handler.capture());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        P2PFrameCodec.write(out, offer());
        EmbeddedChannel channel = new EmbeddedChannel(handler.getAllValues().toArray(ChannelHandler[]::new));
        channel.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(out.toByteArray()));

        assertEquals("session-1", proposalRef.get().getSession().getId());
        assertNull(channel.pipeline().get(P2PFrameDecoder.class), "control decoder must be removed before tunnel bytes");
    }

    @Test
    void installedHandlerReadsSplitOfferFrame() throws Exception {
        Stream stream = mock(Stream.class);
        AtomicReference<SessionProposal> proposalRef = new AtomicReference<>();
        Libp2pSessionResponder responder = new Libp2pSessionResponder((proposal, tunneler) -> {
            proposalRef.set(proposal);
            tunneler.tunnel(proposal.getSession(), new NoopHandler());
        });

        responder.install(stream);

        ArgumentCaptor<ChannelHandler> handler = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(stream, times(2)).pushHandler(handler.capture());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        P2PFrameCodec.write(out, offer());
        byte[] frame = out.toByteArray();
        EmbeddedChannel channel = new EmbeddedChannel(handler.getAllValues().toArray(ChannelHandler[]::new));
        channel.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(frame, 0, 2));
        channel.writeInbound(io.netty.buffer.Unpooled.wrappedBuffer(frame, 2, frame.length - 2));

        assertEquals("session-1", proposalRef.get().getSession().getId());
    }

    private static SessionOffer offer() {
        return SessionOffer.newBuilder()
                .setSessionId("session-1")
                .setEndpoint("endpoint")
                .setPlayer(SessionPlayer.newBuilder()
                        .setAddr("127.0.0.1")
                        .setProfile(SessionGameProfile.newBuilder()
                                .setId("00000000-0000-0000-0000-000000000000")
                                .setName("Player")))
                .setAuth(SessionAuthentication.newBuilder().setPassthrough(false))
                .build();
    }

    private static final class NoopHandler implements com.minekube.connect.tunnel.TunnelConn.Handler {
        @Override
        public void onReceive(byte[] data) {
        }

        @Override
        public void onError(Throwable t) {
        }
    }
}
