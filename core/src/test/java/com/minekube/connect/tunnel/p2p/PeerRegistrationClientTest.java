package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.protobuf.ByteString;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import minekube.connect.v1alpha1.ConnectLibp2P.OfflineMode;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerCapacity;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterChallenge;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterCommit;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterInit;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class PeerRegistrationClientTest {

    @TempDir
    Path tempDir;

    @Test
    void exchangesInitCommitAndResultOnOneStream() throws Exception {
        EndpointPeerIdentity identity = EndpointPeerIdentity.loadOrCreate(tempDir.resolve("native-peer.key"));
        PeerRegistrationHandshake handshake = new PeerRegistrationHandshake(
                identity,
                "endpoint",
                "token",
                "instance",
                Collections.emptyList(),
                OfflineMode.OFFLINE_MODE_ALLOWED,
                Arrays.asList("session", "status"),
                PeerCapacity.newBuilder().setMaxSessions(100).build());
        Stream stream = mock(Stream.class);
        PeerRegistrationClient client = new PeerRegistrationClient(handshake);

        CompletableFuture<PeerRegisterResult> result = client.install(
                stream,
                Collections.singletonList("/ip4/127.0.0.1/tcp/1234/p2p/" + identity.peerId()),
                9,
                1_000);

        ArgumentCaptor<Object> outbound = ArgumentCaptor.forClass(Object.class);
        verify(stream).writeAndFlush(outbound.capture());
        PeerRegisterInit init = P2PFrameCodec.read(
                new ByteBufInputStream((ByteBuf) outbound.getValue()),
                PeerRegisterInit.parser(),
                P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
        assertEquals(identity.peerId(), init.getEndpointPeerId());

        ArgumentCaptor<ChannelHandler> handlers = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(stream, times(2)).pushHandler(handlers.capture());
        EmbeddedChannel channel = new EmbeddedChannel(handlers.getAllValues().toArray(ChannelHandler[]::new));
        channel.writeInbound(frame(PeerRegisterChallenge.newBuilder()
                .setEndpointId("endpoint-id")
                .setEndpointHash("endpoint-hash")
                .setPublisherId("publisher")
                .setPublisherPeerId("publisher-peer")
                .setRegion("local")
                .setKvTtlMs(45_000)
                .setRenewIntervalMs(60_000)
                .setNonce(ByteString.copyFromUtf8("nonce"))
                .build()));

        verify(stream, times(2)).writeAndFlush(outbound.capture());
        Object commitFrame = outbound.getAllValues().get(outbound.getAllValues().size() - 1);
        PeerRegisterCommit commit = P2PFrameCodec.read(
                new ByteBufInputStream((ByteBuf) commitFrame),
                PeerRegisterCommit.parser(),
                P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
        assertEquals(9, commit.getRecord().getSequence());
        assertEquals("publisher-peer", commit.getRecord().getPublisherPeerId());
        assertEquals("local", commit.getRecord().getRegion());

        channel.writeInbound(frame(PeerRegisterResult.newBuilder()
                .setEndpointId("endpoint-id")
                .setEndpointHash("endpoint-hash")
                .setKvRevision(42)
                .build()));

        assertTrue(result.isDone());
        assertEquals(42, result.get(1, TimeUnit.SECONDS).getKvRevision());
    }

    @Test
    void completesClosedFutureWhenRegistrationStreamCloses() throws Exception {
        EndpointPeerIdentity identity = EndpointPeerIdentity.loadOrCreate(tempDir.resolve("native-peer.key"));
        PeerRegistrationHandshake handshake = new PeerRegistrationHandshake(
                identity,
                "endpoint",
                "token",
                "instance",
                Collections.emptyList(),
                OfflineMode.OFFLINE_MODE_ALLOWED,
                Arrays.asList("session", "status"),
                PeerCapacity.newBuilder().setMaxSessions(100).build());
        Stream stream = mock(Stream.class);
        PeerRegistrationClient client = new PeerRegistrationClient(handshake);

        client.install(
                stream,
                Collections.singletonList("/ip4/127.0.0.1/tcp/1234/p2p/" + identity.peerId()),
                9,
                1_000);

        ArgumentCaptor<ChannelHandler> handlers = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(stream, times(2)).pushHandler(handlers.capture());
        EmbeddedChannel channel = new EmbeddedChannel(handlers.getAllValues().toArray(ChannelHandler[]::new));
        channel.writeInbound(frame(PeerRegisterChallenge.newBuilder()
                .setEndpointId("endpoint-id")
                .setEndpointHash("endpoint-hash")
                .setPublisherId("publisher")
                .setPublisherPeerId("publisher-peer")
                .setRegion("local")
                .setKvTtlMs(45_000)
                .setRenewIntervalMs(60_000)
                .setNonce(ByteString.copyFromUtf8("nonce"))
                .build()));
        channel.writeInbound(frame(PeerRegisterResult.newBuilder()
                .setEndpointId("endpoint-id")
                .setEndpointHash("endpoint-hash")
                .setKvRevision(42)
                .build()));

        channel.close().syncUninterruptibly();

        assertTrue(client.closedFuture().isDone());
    }

    @Test
    void computesRenewDelayFromChallenge() {
        assertEquals(20_000, PeerRegistrationClient.renewDelayMillis(PeerRegisterChallenge.newBuilder()
                .setRenewIntervalMs(20_000)
                .setKvTtlMs(45_000)
                .build()));
        assertEquals(22_500, PeerRegistrationClient.renewDelayMillis(PeerRegisterChallenge.newBuilder()
                .setKvTtlMs(45_000)
                .build()));
        assertEquals(1_000, PeerRegistrationClient.renewDelayMillis(PeerRegisterChallenge.newBuilder()
                .setRenewIntervalMs(50)
                .build()));
    }

    private static ByteBuf frame(com.google.protobuf.MessageLite message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        P2PFrameCodec.write(out, message);
        return io.netty.buffer.Unpooled.wrappedBuffer(out.toByteArray());
    }
}
