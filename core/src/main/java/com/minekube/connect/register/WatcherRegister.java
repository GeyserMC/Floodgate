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
import com.minekube.connect.api.SimpleFloodgateApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import com.minekube.connect.watch.SessionProposal.State;
import com.minekube.connect.watch.WatchClient;
import com.minekube.connect.watch.Watcher;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Starts watching for session proposals for connecting players.
 */
public class WatcherRegister {
    @Inject private WatchClient watchClient;
    @Inject private Tunneler tunneler;
    @Inject private PlatformInjector platformInjector;
    @Inject private FloodgateLogger logger;
    @Inject private SimpleFloodgateApi api;

    @Inject
    public void start() {
        watchClient.watch(new WatcherImpl());
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
            logger.info("Reconnecting in {}s ...", RECONNECT_AFTER_ERR.getSeconds());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    start();
                }
            }, RECONNECT_AFTER_ERR.toMillis());
        }
    }

    private final static Duration RECONNECT_AFTER_ERR = Duration.ofSeconds(5);
}
