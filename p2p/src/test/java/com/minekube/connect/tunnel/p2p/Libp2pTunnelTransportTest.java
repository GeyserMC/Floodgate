/*
 * Copyright (c) 2021-2022 Minekube. https://minekube.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minekube.connect.tunnel.P2PTunnelHeader;
import com.minekube.connect.tunnel.TunnelConn;
import io.libp2p.core.Host;
import io.libp2p.core.Stream;
import io.libp2p.core.crypto.KeyType;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;
import org.junit.jupiter.api.Test;

class Libp2pTunnelTransportTest {

    @Test
    void sendsMoxyCompatibleHeaderAndReceivesBytes() throws Exception {
        Responder responder = startResponder();
        try {
            Libp2pTunnelTransport transport = new Libp2pTunnelTransport(tcpOnlyHost());
            try {
                RecordingHandler handler = new RecordingHandler();
                TunnelConn conn = transport.tunnel(responder.address(), "test-session", handler);

                assertEquals(Type.TYPE_LIBP2P, transport.type());
                assertTrue(conn.opened());
                assertArrayEquals(P2PTunnelHeader.encode("test-session"),
                        responder.awaitHeader());
                assertArrayEquals(new byte[] {1, 2, 3}, handler.awaitData());
            } finally {
                transport.close();
            }
        } finally {
            responder.close();
        }
    }

    @Test
    void prepareWarmsConnectionBeforeOpeningSessionStream() throws Exception {
        Responder responder = startResponder();
        try {
            Libp2pTunnelTransport transport = new Libp2pTunnelTransport(tcpOnlyHost());
            try {
                transport.prepare(responder.address());

                assertTrue(transport.hasWarmConnection(responder.address()));
                assertEquals(0, responder.streamCount());

                transport.tunnel(responder.address(), "test-session", new RecordingHandler());

                assertArrayEquals(P2PTunnelHeader.encode("test-session"),
                        responder.awaitHeader());
                assertEquals(1, responder.streamCount());
            } finally {
                transport.close();
            }
        } finally {
            responder.close();
        }
    }

    private static Responder startResponder() throws Exception {
        Responder responder = new Responder(tcpOnlyHost());
        responder.start();
        return responder;
    }

    private static Host tcpOnlyHost() {
        return new HostBuilder(HostBuilder.DefaultMode.None)
                .keyType(KeyType.ED25519)
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getYamux)
                .listen("/ip4/127.0.0.1/tcp/0")
                .build();
    }

    private static final class Responder {
        private final Host host;
        private final CountDownLatch headerLatch = new CountDownLatch(1);
        private final AtomicReference<byte[]> header = new AtomicReference<>();
        private final AtomicInteger streams = new AtomicInteger();

        private Responder(Host host) {
            this.host = host;
        }

        private void start() throws Exception {
            host.addProtocolHandler(new StrictProtocolBinding<Void>(
                    Libp2pTunnelTransport.PROTOCOL_ID,
                    new ProtocolHandler<Void>(Long.MAX_VALUE, Long.MAX_VALUE) {
                        @Override
                        protected CompletableFuture<Void> onStartInitiator(Stream stream) {
                            return CompletableFuture.completedFuture(null);
                        }

                        @Override
                        protected CompletableFuture<Void> onStartResponder(Stream stream) {
                            streams.incrementAndGet();
                            stream.pushHandler(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    header.set(ByteBufUtil.getBytes(
                                            msg, msg.readerIndex(), msg.readableBytes(), false));
                                    headerLatch.countDown();
                                    stream.writeAndFlush(Unpooled.wrappedBuffer(new byte[] {1, 2, 3}));
                                }
                            });
                            return CompletableFuture.completedFuture(null);
                        }
                    }) {
            });
            host.start().get(10, TimeUnit.SECONDS);
        }

        private String address() {
            return host.listenAddresses().stream()
                    .filter(addr -> addr.toString().contains("/tcp/"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no tcp listen address"))
                    .withP2P(host.getPeerId())
                    .toString();
        }

        private byte[] awaitHeader() throws Exception {
            assertTrue(headerLatch.await(5, TimeUnit.SECONDS), "timed out waiting for header");
            return header.get();
        }

        private int streamCount() {
            return streams.get();
        }

        private void close() throws Exception {
            host.stop().get(10, TimeUnit.SECONDS);
        }
    }

    private static final class RecordingHandler implements TunnelConn.Handler {
        private final CountDownLatch dataLatch = new CountDownLatch(1);
        private final AtomicReference<byte[]> data = new AtomicReference<>();

        @Override
        public void onReceive(byte[] data) {
            this.data.set(Arrays.copyOf(data, data.length));
            dataLatch.countDown();
        }

        @Override
        public void onError(Throwable t) {
        }

        private byte[] awaitData() throws Exception {
            assertTrue(dataLatch.await(5, TimeUnit.SECONDS), "timed out waiting for tunnel data");
            return data.get();
        }
    }
}
