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

package org.geysermc.floodgate.register;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.minekube.connect.event.SessionProposeEvent;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import com.minekube.connect.watch.SessionProposal.State;
import com.minekube.connect.watch.WatchClient;
import com.minekube.connect.watch.Watcher;
import io.netty.util.AttributeKey;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.netty.LocalSession;
import org.geysermc.floodgate.platform.listener.EventSink;

/**
 * Starts watching for session proposals for connecting players.
 */
public class WatcherRegister {
    @Inject private WatchClient watchClient;
    @Inject private Tunneler tunneler;
    @Inject private PlatformInjector platformInjector;
    @Inject private EventSink eventSink;
    @Inject private FloodgateLogger logger;
    @Inject private SimpleFloodgateApi api;

    @Inject
    @Named("playerAttribute")
    private AttributeKey<FloodgatePlayer> playerAttribute;

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

            eventSink.fire(new SessionProposeEvent(proposal)).thenAccept(event -> {
                if (event.getSessionProposal().getState() != State.ACCEPTED) {
                    // rejected by event handler
                    return;
                }
                // checking the second time, an event handler could have modified it.
                if (event.getSessionProposal().getSession().getTunnelServiceAddr().isEmpty()) {
                    logger.info("A session proposal event handler emptied the tunnel " +
                            "service address, rejecting it");
                    event.getSessionProposal().reject(Status.newBuilder()
                            .setCode(Code.INTERNAL_VALUE)
                            .setMessage("an internal event handler made " +
                                    "the session proposal invalid")
                            .build());
                    return;
                }
                // Try establishing connection
                new LocalSession(api, tunneler,
                        platformInjector.getServerSocketAddress(),
                        event.getSessionProposal(), playerAttribute
                ).connect();
            });
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }

}
