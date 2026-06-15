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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.platform.util.PlatformUtils;
import com.minekube.connect.tunnel.Tunneler;
import io.libp2p.core.Connection;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.protocol.ProtocolHandler;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import minekube.connect.v1alpha1.ConnectLibp2P.OfflineMode;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerCapacity;
import minekube.connect.v1alpha1.ConnectLibp2P.PeerRegisterResult;

public final class NativeLibp2pEndpoint {
    static final String REGISTER_PROTOCOL_ID = "/minekube/connect/register/1.0.0";
    private static final long START_TIMEOUT_SECONDS = 10;
    private static final long CONNECT_TIMEOUT_SECONDS = 15;
    private static final long STREAM_TIMEOUT_SECONDS = 5;

    private final Path dataDirectory;
    private final ConnectConfig connectConfig;
    private final String connectToken;
    private final PlatformUtils platformUtils;
    private final ConnectLogger logger;
    private final PlatformInjector platformInjector;
    private final SimpleConnectApi api;
    private final AtomicLong sequence = new AtomicLong();

    private NativeLibp2pEndpointConfig nativeConfig;
    private Host host;
    private EndpointPeerIdentity identity;
    private boolean started;

    @Inject
    public NativeLibp2pEndpoint(
            @Named("dataDirectory") Path dataDirectory,
            ConnectConfig connectConfig,
            @Named("connectToken") String connectToken,
            PlatformUtils platformUtils,
            ConnectLogger logger,
            PlatformInjector platformInjector,
            SimpleConnectApi api) {
        this.dataDirectory = dataDirectory;
        this.connectConfig = connectConfig;
        this.connectToken = connectToken;
        this.platformUtils = platformUtils;
        this.logger = logger;
        this.platformInjector = platformInjector;
        this.api = api;
    }

    @Inject
    public synchronized void start() {
        nativeConfig = NativeLibp2pEndpointConfig.fromSystemEnvironment();
        if (!nativeConfig.enabled()) {
            return;
        }
        try {
            identity = EndpointPeerIdentity.loadOrCreate(dataDirectory.resolve("native-libp2p.key"));
            host = Libp2pTunnelTransport.createHost(
                    identity.privateKey(),
                    nativeConfig.listenAddrs().toArray(String[]::new));
            installRegisterProtocol(host);
            installSessionResponder(host);
            await(host.start(), START_TIMEOUT_SECONDS, "start native libp2p endpoint host");
            started = true;
            PeerRegisterResult result = registerOnce();
            logger.info("Native Connect libp2p endpoint registered: "
                    + result.getEndpointId() + " (" + identity.peerId() + ")");
        } catch (Exception e) {
            stop();
            logger.error("Failed to start native Connect libp2p endpoint", e);
        }
    }

    public synchronized void stop() {
        if (!started || host == null) {
            return;
        }
        Host stopping = host;
        host = null;
        started = false;
        await(stopping.stop(), START_TIMEOUT_SECONDS, "stop native libp2p endpoint host");
    }

    private void installSessionResponder(Host host) {
        Libp2pSessionResponder responder = new Libp2pSessionResponder((proposal, tunneler) ->
                new LocalSession(
                        logger,
                        api,
                        tunneler,
                        platformInjector.getServerSocketAddress(),
                        proposal).connect());
        host.addProtocolHandler(new StrictProtocolBinding<Void>(
                Libp2pSessionResponder.PROTOCOL_ID,
                new ProtocolHandler<Void>(Long.MAX_VALUE, Long.MAX_VALUE) {
                    @Override
                    protected CompletableFuture<Void> onStartInitiator(Stream stream) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    protected CompletableFuture<Void> onStartResponder(Stream stream) {
                        responder.install(stream);
                        return CompletableFuture.completedFuture(null);
                    }
                }) {
        });
    }

    static void installRegisterProtocol(Host host) {
        host.addProtocolHandler(new StrictProtocolBinding<Void>(
                REGISTER_PROTOCOL_ID,
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

    private PeerRegisterResult registerOnce() {
        RuntimeException lastError = null;
        for (String address : nativeConfig.registerAddrs()) {
            try {
                Stream stream = openRegisterStream(address);
                PeerRegistrationHandshake handshake = new PeerRegistrationHandshake(
                        identity,
                        connectConfig.getEndpoint(),
                        connectToken,
                        identity.peerId(),
                        connectConfig.getSuperEndpoints() == null
                                ? Collections.emptyList()
                                : connectConfig.getSuperEndpoints(),
                        offlineMode(),
                        Arrays.asList("session", "status"),
                        PeerCapacity.newBuilder()
                                .setMaxSessions(512)
                                .setActiveSessions(platformUtils.getPlayerCount())
                                .build());
                return await(new PeerRegistrationClient(handshake).install(
                                stream,
                                observedAddrs(),
                                sequence.incrementAndGet(),
                                System.currentTimeMillis()),
                        CONNECT_TIMEOUT_SECONDS,
                        "register native libp2p endpoint");
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        throw lastError == null
                ? new IllegalStateException("no native libp2p moxy register addresses configured")
                : lastError;
    }

    private Stream openRegisterStream(String address) {
        Multiaddr multiaddr = Multiaddr.fromString(address);
        PeerId peerId = Objects.requireNonNull(multiaddr.getPeerId(),
                "native libp2p moxy address must include /p2p/<peer-id>");
        Connection connection = await(
                host.getNetwork().connect(peerId, multiaddr),
                CONNECT_TIMEOUT_SECONDS,
                "connect native libp2p moxy peer " + peerId);
        StreamPromise<Object> promise = host.newStream(Arrays.asList(REGISTER_PROTOCOL_ID), connection);
        Stream stream = await(promise.getStream(), STREAM_TIMEOUT_SECONDS, "open native register stream");
        await(stream.getProtocol(), STREAM_TIMEOUT_SECONDS, "negotiate native register protocol");
        return stream;
    }

    private List<String> observedAddrs() {
        if (!nativeConfig.advertiseAddrs().isEmpty()) {
            return nativeConfig.advertiseAddrs();
        }
        return host.listenAddresses().stream()
                .map(addr -> addr.withP2P(host.getPeerId()).toString())
                .collect(Collectors.toList());
    }

    private OfflineMode offlineMode() {
        if (connectConfig.getAllowOfflineModePlayers() != null) {
            return connectConfig.getAllowOfflineModePlayers()
                    ? OfflineMode.OFFLINE_MODE_ALLOWED
                    : OfflineMode.OFFLINE_MODE_DENIED;
        }
        return platformUtils.authType() == PlatformUtils.AuthType.OFFLINE
                ? OfflineMode.OFFLINE_MODE_ALLOWED
                : OfflineMode.OFFLINE_MODE_DENIED;
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
