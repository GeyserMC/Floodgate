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
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.platform.util.PlatformUtils;
import io.libp2p.core.Connection;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.protocol.ProtocolHandler;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import minekube.connect.v1alpha1.ConnectLibp2P.EndpointStatus;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterResult;
import minekube.connect.v1alpha1.ConnectLibp2P.StatusReport;

final class NativeStatusReporter {
    static final String PROTOCOL_ID = "/minekube/connect/status/1.0.0";
    static final String GENERIC_HOST = "__default__";
    static final int JAVA_PORT = 25565;
    static final int DEFAULT_MAX_PLAYERS = 512;
    static final long STATUS_TTL_MS = 60_000;

    private static final long CONNECT_TIMEOUT_SECONDS = 15;
    private static final long STREAM_TIMEOUT_SECONDS = 5;

    private final Host host;
    private final String endpointPeerId;
    private final List<String> moxyAddrs;
    private final PeerRegisterResult registration;
    private final PlatformUtils platformUtils;
    private final ConnectLogger logger;

    NativeStatusReporter(
            Host host,
            String endpointPeerId,
            List<String> moxyAddrs,
            PeerRegisterResult registration,
            PlatformUtils platformUtils,
            ConnectLogger logger) {
        this.host = Objects.requireNonNull(host, "host");
        this.endpointPeerId = Objects.requireNonNull(endpointPeerId, "endpointPeerId");
        this.moxyAddrs = Objects.requireNonNull(moxyAddrs, "moxyAddrs");
        this.registration = Objects.requireNonNull(registration, "registration");
        this.platformUtils = Objects.requireNonNull(platformUtils, "platformUtils");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    void reportSafely() {
        try {
            reportOnce(System.currentTimeMillis());
        } catch (RuntimeException e) {
            logger.error("Failed to report native Connect libp2p status", e);
        }
    }

    static void installStatusProtocol(Host host) {
        host.addProtocolHandler(new StrictProtocolBinding<Void>(
                PROTOCOL_ID,
                new ProtocolHandler<Void>(Long.MAX_VALUE, Long.MAX_VALUE) {
                    @Override
                    protected CompletableFuture<Void> onStartInitiator(Stream stream) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    protected CompletableFuture<Void> onStartResponder(Stream stream) {
                        return CompletableFuture.completedFuture(null);
                    }
                }) {
        });
    }

    void reportOnce(long nowUnixMs) {
        RuntimeException lastError = null;
        StatusReport report = buildReport(nowUnixMs);
        for (String address : moxyAddrs) {
            try {
                Stream stream = openStatusStream(address);
                writeFrame(stream, report);
                return;
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        throw lastError == null
                ? new IllegalStateException("no native libp2p moxy status addresses configured")
                : lastError;
    }

    StatusReport buildReport(long nowUnixMs) {
        String versionName = platformUtils.minecraftVersion();
        if (versionName == null || versionName.trim().isEmpty()) {
            versionName = "Minecraft";
        }
        String implementation = platformUtils.serverImplementationName();
        if (implementation == null || implementation.trim().isEmpty()) {
            implementation = "Minecraft";
        }
        return StatusReport.newBuilder()
                .setEndpointId(registration.getEndpointId())
                .setEndpointHash(registration.getEndpointHash())
                .setEndpointInstanceId(endpointPeerId)
                .setEndpointPeerId(endpointPeerId)
                .setObservedAtUnixMs(nowUnixMs)
                .addStatuses(EndpointStatus.newBuilder()
                        .setEdition("java")
                        .setRequestedHost(GENERIC_HOST)
                        .setRequestedPort(JAVA_PORT)
                        .setOnline(true)
                        .setVersionName(versionName)
                        .setProtocol(0)
                        .setPlayersOnline(Math.max(0, platformUtils.getPlayerCount()))
                        .setPlayersMax(DEFAULT_MAX_PLAYERS)
                        .setDescriptionJson(jsonText(implementation))
                        .setExpiresAtUnixMs(nowUnixMs + STATUS_TTL_MS))
                .build();
    }

    private Stream openStatusStream(String address) {
        Multiaddr multiaddr = Multiaddr.fromString(address);
        PeerId peerId = Objects.requireNonNull(multiaddr.getPeerId(),
                "native libp2p moxy address must include /p2p/<peer-id>");
        Connection connection = await(
                host.getNetwork().connect(peerId, multiaddr),
                CONNECT_TIMEOUT_SECONDS,
                "connect native libp2p moxy peer " + peerId);
        StreamPromise<Object> promise = host.newStream(Arrays.asList(PROTOCOL_ID), connection);
        Stream stream = await(promise.getStream(), STREAM_TIMEOUT_SECONDS, "open native status stream");
        await(stream.getProtocol(), STREAM_TIMEOUT_SECONDS, "negotiate native status protocol");
        return stream;
    }

    private static void writeFrame(Stream stream, MessageLite message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            P2PFrameCodec.write(out, message);
            stream.writeAndFlush(Unpooled.wrappedBuffer(out.toByteArray()));
        } catch (IOException e) {
            throw new IllegalStateException("encode native status report", e);
        }
    }

    private static String jsonText(String text) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
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
}
