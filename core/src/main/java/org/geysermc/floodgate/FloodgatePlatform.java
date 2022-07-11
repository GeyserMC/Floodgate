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

package org.geysermc.floodgate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.UUID;
import net.engio.mbassy.bus.common.PubSubSupport;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.event.PostEnableEvent;
import org.geysermc.floodgate.event.ShutdownEvent;
import org.geysermc.floodgate.link.PlayerLinkLoader;
import org.geysermc.floodgate.module.PostInitializeModule;
import org.geysermc.floodgate.news.NewsChecker;
import org.geysermc.floodgate.util.Metrics;
import org.geysermc.floodgate.util.PostEnableMessages;

public class FloodgatePlatform {
    private static final UUID KEY = UUID.randomUUID();
    @Inject private FloodgateApi api;
    @Inject private PlatformInjector injector;

    @Inject private FloodgateConfig config;
    @Inject private Injector guice;

    @Inject
    public void init(PacketHandlers packetHandlers, HandshakeHandlers handshakeHandlers) {
        PlayerLink link = guice.getInstance(PlayerLinkLoader.class).load();

        InstanceHolder.set(api, link, this.injector, packetHandlers, handshakeHandlers, KEY);

        guice.getInstance(NewsChecker.class).start();
    }

    public void enable(Module... postInitializeModules) throws RuntimeException {
        if (injector == null) {
            throw new RuntimeException("Failed to find the platform injector!");
        }

        try {
            injector.inject();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to inject the packet listener!", exception);
        }

        this.guice = guice.createChildInjector(new PostInitializeModule(postInitializeModules));

        //todo add some kind of auto-load, as this looks kinda weird
        guice.getInstance(PostEnableMessages.class);
        guice.getInstance(Metrics.class);

        guice.getInstance(PubSubSupport.class).publish(new PostEnableEvent());
    }

    public void disable() {
        guice.getInstance(PubSubSupport.class).publish(new ShutdownEvent());

        if (injector != null && injector.canRemoveInjection()) {
            try {
                injector.removeInjection();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to remove the injection!", exception);
            }
        }
    }

    public boolean isProxy() {
        return config.isProxy();
    }
}
