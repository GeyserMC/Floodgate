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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import org.geysermc.api.Geyser;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.PostEnableEvent;
import org.geysermc.floodgate.event.ShutdownEvent;
import org.geysermc.floodgate.module.PostEnableModules;

@Getter(AccessLevel.PROTECTED)
public abstract class FloodgatePlatform {
    private static final UUID KEY = UUID.randomUUID();
    private PlatformInjector injector;

    private FloodgateConfig config;
    @Inject private Injector guice;


    public void load() {
        long startTime = System.currentTimeMillis();

        guice = guice != null ?
                guice.createChildInjector(loadStageModules()) :
                Guice.createInjector(loadStageModules());

        config = guice.getInstance(FloodgateConfig.class);
        injector = guice.getInstance(PlatformInjector.class);

        GeyserApiBase api = guice.getInstance(GeyserApiBase.class);
        InstanceHolder.set(
                guice.getInstance(FloodgateApi.class),
                guice.getInstance(PlayerLink.class),
                injector,
                guice.getInstance(PacketHandlers.class),
                guice.getInstance(HandshakeHandlers.class),
                KEY
        );
        Geyser.set(api);

        long endTime = System.currentTimeMillis();
        guice.getInstance(FloodgateLogger.class)
                .translatedInfo("floodgate.core.finish", endTime - startTime);
    }

    protected abstract List<Module> loadStageModules();

    public void enable() throws RuntimeException {
        if (injector == null) {
            throw new RuntimeException("Failed to find the platform injector!");
        }

        try {
            injector.inject();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to inject the packet listener!", exception);
        }

        this.guice = guice.createChildInjector(new PostEnableModules(postEnableStageModules()));

        guice.getInstance(EventBus.class).fire(new PostEnableEvent());
    }

    protected abstract List<Module> postEnableStageModules();

    public void disable() {
        guice.getInstance(EventBus.class).fire(new ShutdownEvent());

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

    public <T> T getInstance(Class<T> clazz) {
        return guice.getInstance(clazz);
    }
}
