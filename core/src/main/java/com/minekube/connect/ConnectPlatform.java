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

package com.minekube.connect;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.InstanceHolder;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.packet.PacketHandlers;
import com.minekube.connect.config.ConfigHolder;
import com.minekube.connect.config.ConfigLoader;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.module.ConfigLoadedModule;
import com.minekube.connect.module.PostInitializeModule;
import com.minekube.connect.register.WatcherRegister;
import com.minekube.connect.util.Metrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ConnectPlatform {
    private static final UUID KEY = UUID.randomUUID();
    private final ConnectApi api;
    private final PlatformInjector injector;

    private final ConnectLogger logger;

    private ConnectConfig config;
    private Injector guice;

    @Inject
    public ConnectPlatform(
            ConnectApi api,
            PlatformInjector platformInjector,
            ConnectLogger logger,
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
            ConfigHolder configHolder,
            PacketHandlers packetHandlers) {

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

        InstanceHolder.set(api, this.injector, packetHandlers, KEY);
    }

    public boolean enable(Module... postInitializeModules) {
        if (injector == null) {
            logger.error("Failed to find the platform injector!");
            return false;
        }

        try {
            if (!injector.inject()) { // TODO && !bootstrap.getGeyserConfig().isUseDirectConnection()
                logger.error("Failed to inject the packet listener!");
                return false;
            }
        } catch (Exception exception) {
            logger.error("Failed to inject the packet listener!", exception);
            return false;
        }

        this.guice = guice.createChildInjector(new PostInitializeModule(postInitializeModules));

        guice.getInstance(Metrics.class);

        return true;
    }

    public boolean disable() {
        guice.getInstance(CommonPlatformInjector.class).shutdown();
        guice.getInstance(WatcherRegister.class).stop();
        return true;
    }

    public boolean isProxy() {
        return config.isProxy();
    }
}
