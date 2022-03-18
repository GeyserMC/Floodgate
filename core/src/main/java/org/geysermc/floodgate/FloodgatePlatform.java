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
import com.google.inject.name.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.config.loader.ConfigLoader;
import org.geysermc.floodgate.link.PlayerLinkLoader;
import org.geysermc.floodgate.module.ConfigLoadedModule;
import org.geysermc.floodgate.module.PostInitializeModule;
import org.geysermc.floodgate.news.NewsChecker;
import org.geysermc.floodgate.util.PrefixCheckTask;

public class FloodgatePlatform {
    private static final UUID KEY = UUID.randomUUID();
    private final FloodgateApi api;
    private final PlatformInjector injector;

    private final FloodgateLogger logger;

    private FloodgateConfig config;
    private Injector guice;

    @Inject
    public FloodgatePlatform(
            FloodgateApi api,
            PlatformInjector platformInjector,
            FloodgateLogger logger,
            Injector guice) {

        this.api = api;
        this.injector = platformInjector;
        this.logger = logger;
        this.guice = guice;
    }

    @Inject
    public void init(
            @Named("dataDirectory") Path dataDirectory,
            ConfigLoader configLoader,
            FloodgateConfigHolder configHolder,
            PacketHandlers packetHandlers,
            HandshakeHandlers handshakeHandlers) {

        if (!Files.isDirectory(dataDirectory)) {
            try {
                Files.createDirectory(dataDirectory);
            } catch (IOException exception) {
                logger.error("Failed to create the data folder", exception);
                throw new RuntimeException("Failed to create the data folder", exception);
            }
        }

        config = configLoader.load();
        if (config.isDebug()) {
            logger.enableDebug();
        }

        configHolder.set(config);
        guice = guice.createChildInjector(new ConfigLoadedModule(config));
        PlayerLink link = guice.getInstance(PlayerLinkLoader.class).load();

        InstanceHolder.set(api, link, this.injector, packetHandlers, handshakeHandlers, KEY);

        guice.getInstance(NewsChecker.class).start();
    }

    public boolean enable(Module... postInitializeModules) {
        if (injector == null) {
            logger.error("Failed to find the platform injector!");
            return false;
        }

        try {
            if (!injector.inject()) {
                logger.error("Failed to inject the packet listener!");
                return false;
            }
        } catch (Exception exception) {
            logger.error("Failed to inject the packet listener!", exception);
            return false;
        }

        this.guice = guice.createChildInjector(new PostInitializeModule(postInitializeModules));

        PrefixCheckTask.checkAndExecuteDelayed(config, logger);

        return true;
    }

    public boolean disable() {
        if (injector != null && injector.canRemoveInjection()) {
            try {
                if (!injector.removeInjection()) {
                    logger.error("Failed to remove the injection!");
                }
            } catch (Exception exception) {
                logger.error("Failed to remove the injection!", exception);
            }
        }

        guice.getInstance(NewsChecker.class).shutdown();
        api.getPlayerLink().stop();
        return true;
    }

    public boolean isProxy() {
        return config.isProxy();
    }
}
