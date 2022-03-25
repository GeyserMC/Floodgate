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
import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
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

    private WebSocket ws;
    private ExponentialBackOff backOffPolicy;
    private final AtomicBoolean started = new AtomicBoolean();

    @Inject
    public void start() {
        if (started.compareAndSet(false, true)) {
            backOffPolicy = new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(1000) // 1 second
                    .setMaxElapsedTimeMillis(Integer.MAX_VALUE) // 24.8 days
                    .setMaxIntervalMillis(60000 * 5) // 5 minutes
                    .setMultiplier(1.5) // 50% increase per back off
                    .build();
            watch();
        }
    }

    public void stop() {
        if (ws != null) {
            if (started.compareAndSet(true, false)) {
                logger.info("Stopped watching for sessions");
            }
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            if (retryTask != null) {
                retryTask.cancel();
                retryTask = null;
            }
            ws.close(1000, "watcher stopped");
            ws = null;
        }
    }

    private Timer timer;
    private TimerTask retryTask;

    private void retry() {
        if (started.get()) {
            if (retryTask != null) {
                retryTask.cancel();
            }
            if (timer == null) {
                timer = new Timer();
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
            retryTask = new TimerTask();
            logger.info("Reconnecting in {}...",
                    Utils.humanReadableFormat(Duration.ofMillis(millis)));
            timer.schedule(retryTask, millis);
        }
    }

    private class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            if (started.get()) {
                watch();
            }
        }
    }


    private void watch() {
        if (ws != null) {
            ws.close(1000, "watcher is reconnecting");
        }
        ws = watchClient.watch(new WatcherImpl());
    }

    private class WatcherImpl implements Watcher {
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

            if (logger.isDebug()) { // skipping a lot of proposal.toString operations
                logger.debug("Received {}", proposal);
            }

            if (proposal.getState() != State.ACCEPTED) {
                return;
            }

            // Try establishing connection
            new LocalSession(logger, api, tunneler,
                    platformInjector.getServerSocketAddress(),
                    proposal
            ).connect();
        }

        @Override
        public void onError(Throwable t) {
            logger.error("Connection error with WatchService: " +
                            t.getLocalizedMessage() + (
                            t.getCause() == null ? ""
                                    : " (cause: " + t.getCause().getLocalizedMessage() + ")"
                    )
            );
            retry();
        }
    }
}
