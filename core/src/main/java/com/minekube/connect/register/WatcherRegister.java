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

package com.minekube.connect.register;

import com.google.inject.Inject;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.util.Utils;
import com.minekube.connect.util.backoff.BackOff;
import com.minekube.connect.util.backoff.ExponentialBackOff;
import com.minekube.connect.watch.SessionProposal;
import com.minekube.connect.watch.SessionProposal.State;
import com.minekube.connect.watch.WatchClient;
import com.minekube.connect.watch.Watcher;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.WebSocket;

/**
 * Starts watching for session proposals for connecting players.
 */
public class WatcherRegister {
    @Inject private WatchClient watchClient;
    @Inject private Tunneler tunneler;
    @Inject private PlatformInjector platformInjector;
    @Inject private ConnectLogger logger;
    @Inject private SimpleConnectApi api;

    // volatile: written from injection thread (start/stop) and read from the
    // scheduler thread (retry) and OkHttp dispatcher (WatcherImpl callbacks).
    private volatile WebSocket ws;
    private ExponentialBackOff backOffPolicy;
    private final AtomicBoolean started = new AtomicBoolean();

    // Lazily created in start() so a stop()/start() cycle reuses cleanly,
    // and so the daemon thread isn't allocated if start() is never called.
    // java.util.Timer would leak one OS thread per reconnect cycle.
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> retryFuture;

    @Inject
    public void start() {
        if (started.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    new DefaultThreadFactory("connect-watcher-scheduler", true));
            backOffPolicy = new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(1000) // 1 second
                    .setMaxElapsedTimeMillis(Integer.MAX_VALUE) // 24.8 days
                    .setMaxIntervalMillis(60000 * 5) // 5 minutes
                    .setMultiplier(1.5) // 50% increase per back off
                    .build();
            watch();
        }
    }

    public void resetBackOff() {
        backOffPolicy.reset();
    }

    public void stop() {
        // Gate the whole teardown so a concurrent stop() races safely.
        // A stop() before start() is a no-op (started is already false).
        if (!started.compareAndSet(true, false)) {
            return;
        }
        logger.info("Stopped watching for sessions");
        if (retryFuture != null) {
            retryFuture.cancel(false);
            retryFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (ws != null) {
            ws.close(1000, "watcher stopped");
            ws = null;
        }
    }

    private void retry() {
        if (!started.get()) {
            return;
        }
        if (retryFuture != null) {
            retryFuture.cancel(false);
        }
        long millis;
        try {
            millis = backOffPolicy.nextBackOffMillis();
            if (millis == BackOff.STOP) {
                stop();
                return;
            }
        } catch (IOException e) {
            logger.error("nextBackOffMillis error", e);
            return;
        }
        // Snapshot to avoid NPE if stop() races with a late callback that triggered retry().
        ScheduledExecutorService s = scheduler;
        if (s == null) {
            return;
        }
        logger.info("Trying to reconnect in {}...",
                Utils.humanReadableFormat(Duration.ofMillis(millis)));
        retryFuture = s.schedule(() -> {
            if (started.get()) {
                watch();
            }
        }, millis, TimeUnit.MILLISECONDS);
    }

    private void watch() {
        if (ws != null) {
            ws.close(1000, "watcher is reconnecting");
        }
        ws = watchClient.watch(new WatcherImpl());
    }

    private class WatcherImpl implements Watcher {

        @Override
        public void onOpen() {
            logger.translatedInfo("connect.watch.started");
            startResetBackOffTimer();
        }

        @Override
        public void onProposal(SessionProposal proposal) {
            if (proposal.getSession().getTunnelServiceAddr().isEmpty()) {
                logger.info("Got session proposal with empty tunnel service address " +
                        "from WatchService, rejecting it");
                proposal.reject(Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT_VALUE)
                        .setMessage("tunnel service address must not be empty")
                        .build());
                return;
            }
            if (proposal.getSession().getPlayer().getAddr().isEmpty()) {
                logger.info("Got session proposal with empty player address " +
                        "from WatchService, rejecting it");
                proposal.reject(Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT_VALUE)
                        .setMessage("player address must not be empty")
                        .build());
                return;
            }

            if (logger.isDebug()) {
                logger.debug("Received {}", proposal);
            }

            if (proposal.getState() != State.ACCEPTED) {
                return;
            }

            new LocalSession(logger, api, tunneler,
                    platformInjector.getServerSocketAddress(),
                    proposal
            ).connect();
        }

        @Override
        public void onError(Throwable t) {
            logger.error("Connection error with WatchService: " +
                            t + (
                            t.getCause() == null ? ""
                                    : " (cause: " + t.getCause().toString() + ")"
                    )
            );
            cancelResetBackOffTimer();
            retry();
        }

        @Override
        public void onCompleted() {
            cancelResetBackOffTimer();
            retry();
        }

        private volatile ScheduledFuture<?> resetBackOffFuture;

        void startResetBackOffTimer() {
            cancelResetBackOffTimer();
            // Snapshot: a late onOpen after stop() can land here with scheduler == null.
            ScheduledExecutorService s = scheduler;
            if (s == null || !started.get()) {
                return;
            }
            resetBackOffFuture = s.schedule(() -> {
                if (started.get()) {
                    resetBackOff();
                }
            }, Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        }

        void cancelResetBackOffTimer() {
            if (resetBackOffFuture != null) {
                resetBackOffFuture.cancel(false);
                resetBackOffFuture = null;
            }
        }
    }
}
