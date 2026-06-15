package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.libp2p.core.Connection;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.StreamPromise;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndpointLibp2pHostTest {

    @TempDir
    Path tempDir;

    @Test
    void createsHostWithPersistedEndpointIdentity() throws Exception {
        EndpointPeerIdentity identity = EndpointPeerIdentity.loadOrCreate(tempDir.resolve("native-peer.key"));

        Host host = Libp2pTunnelTransport.createHost(identity.privateKey());
        try {
            assertEquals(identity.peerId(), host.getPeerId().toString());
            host.start().get(10, TimeUnit.SECONDS);
        } finally {
            host.stop().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void endpointHostCanOpenRegisterProtocolStream() throws Exception {
        endpointHostCanOpenProtocolStream(NativeLibp2pEndpoint.REGISTER_PROTOCOL_ID, host ->
                NativeLibp2pEndpoint.installRegisterProtocol(host));
    }

    @Test
    void endpointHostCanOpenStatusProtocolStream() throws Exception {
        endpointHostCanOpenProtocolStream(NativeStatusReporter.PROTOCOL_ID, host ->
                NativeStatusReporter.installStatusProtocol(host));
    }

    private void endpointHostCanOpenProtocolStream(String protocolID, java.util.function.Consumer<Host> installer) throws Exception {
        Host endpoint = Libp2pTunnelTransport.createHost(
                EndpointPeerIdentity.loadOrCreate(tempDir.resolve("endpoint.key")).privateKey(),
                "/ip4/127.0.0.1/tcp/0");
        Host moxy = Libp2pTunnelTransport.createHost(
                EndpointPeerIdentity.loadOrCreate(tempDir.resolve("moxy.key")).privateKey(),
                "/ip4/127.0.0.1/tcp/0");
        CountDownLatch accepted = new CountDownLatch(1);

        installer.accept(endpoint);
        moxy.addProtocolHandler(new io.libp2p.core.multistream.StrictProtocolBinding<Void>(
                protocolID,
                new io.libp2p.protocol.ProtocolHandler<Void>(Long.MAX_VALUE, Long.MAX_VALUE) {
                    @Override
                    protected CompletableFuture<Void> onStartInitiator(Stream stream) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    protected CompletableFuture<Void> onStartResponder(Stream stream) {
                        accepted.countDown();
                        return CompletableFuture.completedFuture(null);
                    }
                }) {
        });

        try {
            endpoint.start().get(10, TimeUnit.SECONDS);
            moxy.start().get(10, TimeUnit.SECONDS);

            String address = moxy.listenAddresses().get(0).withP2P(moxy.getPeerId()).toString();
            Connection connection = endpoint.getNetwork()
                    .connect(PeerId.fromBase58(moxy.getPeerId().toBase58()), io.libp2p.core.multiformats.Multiaddr.fromString(address))
                    .get(10, TimeUnit.SECONDS);
            StreamPromise<Object> promise = endpoint.newStream(
                    Arrays.asList(protocolID),
                    connection);
            Stream stream = promise.getStream().get(10, TimeUnit.SECONDS);
            stream.getProtocol().get(10, TimeUnit.SECONDS);

            assertTrue(accepted.await(5, TimeUnit.SECONDS), "register protocol was not accepted");
        } finally {
            endpoint.stop().get(10, TimeUnit.SECONDS);
            moxy.stop().get(10, TimeUnit.SECONDS);
        }
    }
}
