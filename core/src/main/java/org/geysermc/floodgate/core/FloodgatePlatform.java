/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.Qualifier;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.geysermc.api.Geyser;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.event.FloodgateEventBus;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.core.config.ConfigLoader;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.config.Properties;
import org.geysermc.floodgate.core.database.loader.DatabaseLoader;
import org.geysermc.floodgate.core.event.EventBus;
import org.geysermc.floodgate.core.event.lifecycle.PostEnableEvent;
import org.geysermc.floodgate.core.event.lifecycle.ShutdownEvent;
import org.geysermc.floodgate.core.util.GlobalBeanCache;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public abstract class FloodgatePlatform {
    private static final UUID KEY = UUID.randomUUID();

    private final LibraryManager manager;
    private ApplicationContext context;
    private PlatformInjector injector;

    protected FloodgatePlatform(LibraryManager manager) {
        this.manager = manager;
    }

    protected void onContextCreated(ApplicationContext context) {
    }

    public void load() {
        long startTime = System.currentTimeMillis();

        GlobalBeanCache.cacheIfAbsent("libraryManager", () -> manager);

        context = ApplicationContext.builder(manager.classLoader())
                .singletons(manager)
                .properties(Map.of(
                        "platform.proxy", isProxy()
                ))
                .propertySources(Properties.defaults())
                .environmentPropertySource(false)
                .eagerInitSingletons(true)
                .build();
        onContextCreated(context);

        // load and register config and database related stuff
        var dataDirectory = context.getBean(Path.class, Qualifiers.byName("dataDirectory"));
        FloodgateConfig config = ConfigLoader.load(dataDirectory, isProxy(), context);
        DatabaseLoader.load(config, manager, dataDirectory, context);

        context.start();

        injector = context.getBean(PlatformInjector.class);

        GeyserApiBase api = context.getBean(GeyserApiBase.class);
        InstanceHolder.set(
                context.getBean(FloodgateApi.class),
                null, // todo context.getBean(PlayerLink.class),
                context.getBean(FloodgateEventBus.class),
                injector,
                context.getBean(PacketHandlers.class),
                context.getBean(HandshakeHandlers.class),
                KEY
        );
        Geyser.set(api);

        long endTime = System.currentTimeMillis();
        context.getBean(FloodgateLogger.class)
                .translatedInfo("floodgate.core.finish", endTime - startTime);
    }

    public void enable() throws RuntimeException {
        if (injector == null) {
            throw new RuntimeException("Failed to find the platform injector!");
        }

        try {
            injector.inject();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to inject the packet listener!", exception);
        }

        context.getBean(EventBus.class).fire(new PostEnableEvent());
    }

    public void disable() {
        context.getBean(EventBus.class).fire(new ShutdownEvent());

        if (injector != null && injector.canRemoveInjection()) {
            try {
                injector.removeInjection();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to remove the injection!", exception);
            }
        }

        context.close();
    }

    abstract public boolean isProxy();

    public <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
    public <T> T getBean(Class<T> clazz, Qualifier<T> qualifier) {
        return context.getBean(clazz, qualifier);
    }
    public <T, R extends T> R getBean(Argument<T> clazz, Qualifier<T> qualifier) {
        //noinspection unchecked
        return (R) context.getBean(clazz, qualifier);
    }
}
