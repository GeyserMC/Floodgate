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

import com.google.inject.Inject;
import com.minekube.connect.tunnel.P2PTunnelHeader;
import com.minekube.connect.tunnel.TunnelClientTransport;
import com.minekube.connect.tunnel.TunnelConn;
import io.libp2p.core.Connection;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.crypto.KeyType;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.quic.QuicConfig;
import io.libp2p.transport.quic.QuicTransport;
import io.libp2p.transport.tcp.TcpTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;

public final class Libp2pTunnelTransport implements TunnelClientTransport {
    public static final String PROTOCOL_ID = "/minekube/connect/tunnel/1.0.0";

    private static final long START_TIMEOUT_SECONDS = 10;
    private static final long CONNECT_TIMEOUT_SECONDS = 15;
    private static final long STREAM_TIMEOUT_SECONDS = 5;

    private final Host host;
    private final ConcurrentMap<PeerId, Connection> warmConnections = new ConcurrentHashMap<>();

    @Inject
    public Libp2pTunnelTransport() {
        this(createHost());
    }

    Libp2pTunnelTransport(Host host) {
        this.host = Objects.requireNonNull(host, "host");
        this.host.addProtocolHandler(new TunnelProtocolBinding());
        await(host.start(), START_TIMEOUT_SECONDS, "start libp2p host");
    }

    static Host createHost() {
        return new HostBuilder(HostBuilder.DefaultMode.None)
                .keyType(KeyType.ED25519)
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getYamux)
                .secureTransport((priv, protocols) ->
                        QuicTransport.Ed25519(priv, protocols, new QuicConfig()))
                .build();
    }

    @Override
    public Type type() {
        return Type.TYPE_LIBP2P;
    }

    @Override
    public void prepare(String address) {
        if (address == null || address.isEmpty()) {
            return;
        }

        Multiaddr multiaddr = Multiaddr.fromString(address);
        PeerId peerId = requirePeerId(multiaddr, address);
        warmConnections.compute(peerId, (ignored, existing) -> {
            if (isHealthy(existing)) {
                return existing;
            }
            return connect(peerId, multiaddr);
        });
    }

    boolean hasWarmConnection(String address) {
        Multiaddr multiaddr = Multiaddr.fromString(address);
        PeerId peerId = requirePeerId(multiaddr, address);
        return isHealthy(warmConnections.get(peerId));
    }

    @Override
    public TunnelConn tunnel(String address, String sessionId, TunnelConn.Handler handler) {
        Objects.requireNonNull(handler, "handler");
        byte[] header = P2PTunnelHeader.encode(sessionId);
        Multiaddr multiaddr = Multiaddr.fromString(address);
        PeerId peerId = requirePeerId(multiaddr, address);
        Connection connection = warmConnection(peerId, multiaddr);
        Stream stream = openStream(connection);
        try {
            stream.pushHandler(new InboundBytesHandler(handler));
            stream.writeAndFlush(Unpooled.wrappedBuffer(header));
            return new Libp2pTunnelConn(stream);
        } catch (RuntimeException e) {
            stream.close();
            throw e;
        }
    }

    @Override
    public void close() {
        warmConnections.clear();
        await(host.stop(), START_TIMEOUT_SECONDS, "stop libp2p host");
    }

    private Connection warmConnection(PeerId peerId, Multiaddr multiaddr) {
        prepare(multiaddr.toString());
        Connection connection = warmConnections.get(peerId);
        if (!isHealthy(connection)) {
            throw new IllegalStateException("libp2p connection is not warm for " + multiaddr);
        }
        return connection;
    }

    private Connection connect(PeerId peerId, Multiaddr multiaddr) {
        return await(host.getNetwork().connect(peerId, multiaddr),
                CONNECT_TIMEOUT_SECONDS, "connect libp2p peer " + peerId);
    }

    private Stream openStream(Connection connection) {
        StreamPromise<Object> promise = host.newStream(Arrays.asList(PROTOCOL_ID), connection);
        Stream stream = await(promise.getStream(), STREAM_TIMEOUT_SECONDS, "open libp2p tunnel stream");
        await(stream.getProtocol(), STREAM_TIMEOUT_SECONDS, "negotiate libp2p tunnel protocol");
        return stream;
    }

    private static PeerId requirePeerId(Multiaddr multiaddr, String address) {
        PeerId peerId = multiaddr.getPeerId();
        if (peerId == null) {
            throw new IllegalArgumentException("p2p address must include /p2p/<peer-id>: " + address);
        }
        return peerId;
    }

    private static boolean isHealthy(Connection connection) {
        return connection != null && !connection.closeFuture().isDone();
    }

    private static <T> T await(CompletableFuture<T> future, long timeoutSeconds, String action) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("failed to " + action, e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("failed to " + action, e);
        } catch (Exception e) {
            throw new IllegalStateException("failed to " + action, e);
        }
    }

    private static final class Libp2pTunnelConn extends TunnelConn {
        private final Stream stream;

        private Libp2pTunnelConn(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void write(byte[] data) {
            stream.writeAndFlush(Unpooled.wrappedBuffer(data));
        }

        @Override
        public void close(Throwable t) {
            stream.close();
        }

        @Override
        public boolean opened() {
            return true;
        }
    }

    private static final class InboundBytesHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final TunnelConn.Handler handler;

        private InboundBytesHandler(TunnelConn.Handler handler) {
            this.handler = handler;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            handler.onReceive(ByteBufUtil.getBytes(msg, msg.readerIndex(), msg.readableBytes(), false));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            handler.onError(cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            handler.onClose();
            super.channelInactive(ctx);
        }
    }

    private static final class TunnelProtocolBinding extends StrictProtocolBinding<Void> {
        private TunnelProtocolBinding() {
            super(PROTOCOL_ID, new TunnelProtocolHandler());
        }
    }

    private static final class TunnelProtocolHandler extends ProtocolHandler<Void> {
        private TunnelProtocolHandler() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        protected CompletableFuture<Void> onStartInitiator(Stream stream) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> onStartResponder(Stream stream) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
