/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.network.netty;

import com.minekube.connect.tunnel.TunnelConn;
import com.minekube.connect.tunnel.TunnelConn.Handler;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import io.grpc.protobuf.StatusProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Player;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Manages a Minecraft Java session over our LocalChannel implementation.
 */
@RequiredArgsConstructor
public final class LocalSession {
    private static final int CONNECTION_TIMEOUT = (int) Duration.ofSeconds(30).toMillis();

    private static DefaultEventLoopGroup DEFAULT_EVENT_LOOP_GROUP;
    private static PreferredDirectByteBufAllocator PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = null;

    private final SimpleFloodgateApi api;
    private final Tunneler tunneler;
    private final SocketAddress targetAddress; // The server we are connecting to
    private final SessionProposal sessionProposal;
    private final AttributeKey<FloodgatePlayer> playerAttribute;

    private TunnelConn tunnelConn;

    public void connect() {
        if (tunnelConn != null) {
            throw new IllegalStateException("Connection has already been connected.");
        }

        if (DEFAULT_EVENT_LOOP_GROUP == null) {
            DEFAULT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup();
        }

        Player p = sessionProposal.getSession().getPlayer();
        FloodgatePlayer player = new FloodgatePlayerImpl(
                p.getProfile(),
                UUID.fromString(p.getProfile().getId()),
                "", // TODO extract from http accept language header
                p.getAddr()
        );

        LocalSession it = this;
        try {
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(LocalChannelWithRemoteAddress.class);
            bootstrap.handler(new ChannelInitializer<LocalChannelWithRemoteAddress>() {
                        @Override
                        public void initChannel(@NotNull LocalChannelWithRemoteAddress channel) {
                            String clientIp = sessionProposal.getSession().getPlayer().getAddr();
                            if (!clientIp.isEmpty()) {
                                channel.setSpoofedAddress(new InetSocketAddress(clientIp, 0));
                            }

                            channel.attr(playerAttribute).set(player);

                            // Start tunnel connection
                            tunnelConn = tunneler.tunnel(
                                    sessionProposal.getSession().getTunnelServiceAddr(),
                                    new Handler() {
                                        @Override
                                        public void onReceive(byte[] data) {
                                            // forward to downstream server
                                            channel.writeAndFlush(data);
                                        }

                                        @Override
                                        public void onError(Throwable t) {
                                            it.exceptionCaught(t);
                                        }

                                        @Override
                                        public void onClose() {
                                            api.setPendingRemove(player);
                                            // disconnect from server
                                            channel.disconnect();
                                            tunnelConn.close();
                                            tunnelConn = null;
                                        }
                                    }
                            );

                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("connect-tunnel", new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(@NotNull ChannelHandlerContext ctx,
                                                        @NotNull Object msg) {

                                    if (msg instanceof ByteBuf) {
                                        ByteBuf buf = (ByteBuf) msg;
                                        byte[] bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(),
                                                buf.readableBytes(), false);
                                        tunnelConn.write(bytes);
                                    } else {
                                        ctx.fireChannelRead(msg);
                                    }
                                }
                            });
                        }
                    })
                    .group(DEFAULT_EVENT_LOOP_GROUP)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT);

            if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR != null) {
                bootstrap.option(ChannelOption.ALLOCATOR, PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR);
            }

            bootstrap.remoteAddress(targetAddress).connect().addListener((future) -> {
                if (!future.isSuccess()) {
                    exceptionCaught(future.cause());
                    return;
                }
                api.addPlayer(player);
            });
        } catch (Throwable t) {
            exceptionCaught(t);
        }
    }

    private void exceptionCaught(Throwable cause) {
        // Close tunnel stream if there is one
        if (tunnelConn != null) {
            tunnelConn.close();
            tunnelConn = null;
        }
        // Reject session proposal in case we are still able to.
        sessionProposal.reject(StatusProto.fromThrowable(cause));
    }

    /**
     * Should only be called when direct ByteBufs should be preferred. At this moment, this should
     * only be called on BungeeCord.
     */
    public static void createDirectByteBufAllocator() {
        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR == null) {
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = new PreferredDirectByteBufAllocator();
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR.updateAllocator(ByteBufAllocator.DEFAULT);
        }
    }
}
