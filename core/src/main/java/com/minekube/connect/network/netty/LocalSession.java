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

package com.minekube.connect.network.netty;

import com.google.common.base.Preconditions;
import com.minekube.connect.api.SimpleFloodgateApi;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.api.player.Auth;
import com.minekube.connect.api.player.FloodgatePlayer;
import com.minekube.connect.api.player.GameProfileProperty;
import com.minekube.connect.player.FloodgatePlayerImpl;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import com.minekube.connect.watch.SessionProposal.State;
import io.grpc.protobuf.StatusProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Player;
import minekube.connect.v1alpha1.WatchServiceOuterClass.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Manages a Minecraft Java session over our LocalChannel implementation.
 */
@RequiredArgsConstructor
public final class LocalSession {
    private static final int CONNECTION_TIMEOUT = (int) Duration.ofSeconds(30).toMillis();

    private static DefaultEventLoopGroup DEFAULT_EVENT_LOOP_GROUP;
    private static PreferredDirectByteBufAllocator PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = null;

    private final FloodgateLogger logger;
    private final SimpleFloodgateApi api;
    private final Tunneler tunneler;
    private final SocketAddress targetAddress; // The server we are connecting to
    private final SessionProposal sessionProposal;

    private final AtomicBoolean connectOnce = new AtomicBoolean();

    /**
     * Every {@link LocalSession} attaches connection context to a {@link Channel} and can be
     * extracted using {@link LocalSession#context(Channel)}.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class Context {
        final @NotNull FloodgatePlayer player;
        final @NotNull InetSocketAddress spoofedAddress; // real client address
    }

    /**
     * Returns connection {@link Context} of a channel if available.
     *
     * @param channel the channel
     * @return the session context of a connection if available
     */
    public static Optional<Context> context(Channel channel) {
        if (channel instanceof ChannelWrapper) {
            return Optional.of(((ChannelWrapper) channel).getContext());
        }
        if (channel instanceof LocalChannelWrapper) {
            return Optional.of(((LocalChannelWrapper) channel).wrapper().getContext());
        }
        return Optional.empty();
    }

    /**
     * See {@link LocalSession#context(Channel)}
     */
    public static void context(Channel channel, Consumer<Context> ifPresent) {
        context(channel).ifPresent(ifPresent);
    }

    public void connect() {
        if (sessionProposal.getState() != State.ACCEPTED) {
            throw new IllegalStateException("Session proposal has already been rejected.");
        }
        if (!connectOnce.compareAndSet(false, true)) {
            throw new IllegalStateException("Connection has already been connected.");
        }

        final Context context = new Context(
                fromProto(sessionProposal.getSession()),
                createAddress(sessionProposal.getSession().getPlayer().getAddr())
        );

        if (DEFAULT_EVENT_LOOP_GROUP == null) {
            DEFAULT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup();
        }

        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(LocalChannelWithSessionContext.class)
                .handler(new ChannelInitializer<LocalChannelWithSessionContext>() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                            throws Exception {
                        LocalSession.this.exceptionCaught(cause);
                        super.exceptionCaught(ctx, cause);
                    }

                    @Override
                    public void initChannel(@NotNull LocalChannelWithSessionContext channel) {
                        channel.setContext(context);
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("connect-tunnel", new LocalChannelInboundHandler(
                                logger, api, tunneler, context.player, sessionProposal));
                    }
                })
                .group(DEFAULT_EVENT_LOOP_GROUP)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT);

        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR != null) {
            bootstrap.option(ChannelOption.ALLOCATOR, PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR);
        }

        logger.debug("Connecting {} to local downstream server {}",
                context.player.getUsername(), targetAddress);
        bootstrap
                .remoteAddress(targetAddress)
                .connect()
                .addListener((future) -> {
                    if (!future.isSuccess()) {
                        exceptionCaught(future.cause());
                        return;
                    }
                    api.addPlayer(context.player);
                });
    }

    private void exceptionCaught(Throwable cause) {
        cause.printStackTrace();
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

    private static FloodgatePlayer fromProto(Session s) {
        Player p = s.getPlayer();
        return new FloodgatePlayerImpl(
                s.getId(),
                new Auth(s.getAuth().getPassthrough()),
                UUID.fromString(p.getProfile().getId()),
                p.getProfile().getName(),
                p.getProfile().getPropertiesList().stream()
                        .map(property -> new GameProfileProperty(
                                property.getName(),
                                property.getValue(),
                                property.getSignature()))
                        .collect(Collectors.toList()),
                "", // TODO extract from http accept language header
                p.getAddr()
        );
    }

    private static InetSocketAddress createAddress(String addr) {
        Preconditions.checkNotNull(addr, "player address must not be null");
        Preconditions.checkArgument(!addr.isEmpty(), "player address must not be empty");
        InetSocketAddress address = new InetSocketAddress(addr, 0);
        Preconditions.checkArgument(!address.isUnresolved(),
                "invalid player address (must not contain a port): %s", addr);
        return address;
    }
}
