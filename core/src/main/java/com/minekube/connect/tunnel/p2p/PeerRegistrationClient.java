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
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import com.google.protobuf.MessageLite;
import io.libp2p.core.Stream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterChallenge;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterResult;

final class PeerRegistrationClient {
    private final PeerRegistrationHandshake handshake;
    private final ScheduledExecutorService renewExecutor;
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    private volatile Stream stream;

    PeerRegistrationClient(PeerRegistrationHandshake handshake) {
        this.handshake = Objects.requireNonNull(handshake, "handshake");
        this.renewExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "connect-native-libp2p-registration-renew");
            thread.setDaemon(true);
            return thread;
        });
    }

    CompletableFuture<PeerRegisterResult> install(
            Stream stream,
            List<String> observedAddrs,
            long sequence,
            long nowUnixMs) {
        this.stream = stream;
        CompletableFuture<PeerRegisterResult> result = new CompletableFuture<>();
        P2PFrameDecoder<PeerRegisterChallenge> challengeDecoder = new P2PFrameDecoder<>(
                PeerRegisterChallenge.parser(),
                P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
        stream.pushHandler(challengeDecoder);
        stream.pushHandler(new ChallengeHandler(
                stream,
                challengeDecoder,
                observedAddrs,
                sequence,
                nowUnixMs,
                result));
        writeFrame(stream, handshake.init(observedAddrs));
        return result;
    }

    CompletableFuture<Void> closedFuture() {
        return closed;
    }

    void close() {
        renewExecutor.shutdownNow();
        Stream current = stream;
        if (current != null) {
            current.close();
        }
        closed.complete(null);
    }

    private void installResultHandler(
            ChannelHandlerContext ctx,
            Stream stream,
            PeerRegisterChallenge challenge,
            List<String> observedAddrs,
            long sequence,
            CompletableFuture<PeerRegisterResult> result) {
        P2PFrameDecoder<PeerRegisterResult> resultDecoder = new P2PFrameDecoder<>(
                PeerRegisterResult.parser(),
                P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
        ctx.pipeline().addLast(resultDecoder);
        ctx.pipeline().addLast(new ResultHandler(stream, challenge, observedAddrs, sequence, result));
    }

    private static void writeFrame(Stream stream, MessageLite message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            P2PFrameCodec.write(out, message);
            stream.writeAndFlush(Unpooled.wrappedBuffer(out.toByteArray()));
        } catch (IOException e) {
            throw new IllegalStateException("encode native registration frame", e);
        }
    }

    private final class ChallengeHandler extends SimpleChannelInboundHandler<PeerRegisterChallenge> {
        private final Stream stream;
        private final ChannelHandler decoder;
        private final List<String> observedAddrs;
        private final long sequence;
        private final long nowUnixMs;
        private final CompletableFuture<PeerRegisterResult> result;

        private ChallengeHandler(
                Stream stream,
                ChannelHandler decoder,
                List<String> observedAddrs,
                long sequence,
                long nowUnixMs,
                CompletableFuture<PeerRegisterResult> result) {
            this.stream = stream;
            this.decoder = decoder;
            this.observedAddrs = observedAddrs;
            this.sequence = sequence;
            this.nowUnixMs = nowUnixMs;
            this.result = result;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, PeerRegisterChallenge challenge) {
            ctx.pipeline().remove(this);
            ctx.pipeline().remove(decoder);
            writeFrame(stream, handshake.commit(challenge, observedAddrs, sequence, nowUnixMs));
            installResultHandler(ctx, stream, challenge, observedAddrs, sequence, result);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            result.completeExceptionally(cause);
            closed.completeExceptionally(cause);
            stream.close();
            ctx.close();
        }
    }

    static long renewDelayMillis(PeerRegisterChallenge challenge) {
        if (challenge.getRenewIntervalMs() > 0) {
            return Math.max(1_000, challenge.getRenewIntervalMs());
        }
        if (challenge.getKvTtlMs() > 0) {
            return Math.max(1_000, challenge.getKvTtlMs() / 2);
        }
        return 22_500;
    }

    private final class ResultHandler extends SimpleChannelInboundHandler<PeerRegisterResult> {
        private final Stream stream;
        private final PeerRegisterChallenge challenge;
        private final List<String> observedAddrs;
        private final AtomicLong sequence;
        private final CompletableFuture<PeerRegisterResult> result;

        private ResultHandler(
                Stream stream,
                PeerRegisterChallenge challenge,
                List<String> observedAddrs,
                long sequence,
                CompletableFuture<PeerRegisterResult> result) {
            this.stream = stream;
            this.challenge = challenge;
            this.observedAddrs = observedAddrs;
            this.sequence = new AtomicLong(sequence);
            this.result = result;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, PeerRegisterResult msg) {
            result.complete(msg);
            scheduleRenew();
        }

        private void scheduleRenew() {
            renewExecutor.schedule(() -> {
                if (!stream.closeFuture().isDone()) {
                    writeFrame(stream, handshake.commit(
                            challenge,
                            observedAddrs,
                            sequence.incrementAndGet(),
                            System.currentTimeMillis()));
                }
            }, renewDelayMillis(challenge), TimeUnit.MILLISECONDS);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            result.completeExceptionally(cause);
            renewExecutor.shutdownNow();
            closed.completeExceptionally(cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            renewExecutor.shutdownNow();
            closed.complete(null);
            super.channelInactive(ctx);
        }
    }
}
