/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import lombok.AccessLevel;
import lombok.Getter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.loader.ConfigLoader;
import org.geysermc.floodgate.link.PlayerLinkLoader;
import org.geysermc.floodgate.module.ConfigLoadedModule;
import org.geysermc.floodgate.module.PostInitializeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FloodgatePlatform {
    private static final UUID KEY = UUID.randomUUID();

    @Getter(AccessLevel.PROTECTED)
    private final FloodgateConfig config;
    private final FloodgateApi api;

    @Getter(AccessLevel.PROTECTED)
    private final FloodgateLogger logger;

    private final Injector guice;

    @Inject private PlatformInjector injector;

    @Inject
    public FloodgatePlatform(@Named("dataDirectory") Path dataDirectory, FloodgateApi api,
                             ConfigLoader configLoader, PlayerLinkLoader playerLinkLoader,
                             HandshakeHandler handshakeHandler, FloodgateLogger logger,
                             Injector injector) {
        this.api = api;
        this.logger = logger;

        if (!Files.isDirectory(dataDirectory)) {
            try {
                Files.createDirectory(dataDirectory);
            } catch (Exception exception) {
                logger.error("Failed to create the data folder", exception);
                throw new RuntimeException("Failed to create the data folder");
            }
        }

        config = configLoader.load();

        // make the config available for other classes
        guice = injector.createChildInjector(new ConfigLoadedModule(config, api));

        guice.injectMembers(playerLinkLoader);
        guice.injectMembers(handshakeHandler);

        PlayerLink link = playerLinkLoader.load();

        InstanceHolder.setInstance(api, link, KEY);
    }

    public boolean enable(Module... postCreateModules) {
        if (injector == null) {
            getLogger().error("Failed to find the platform injector!");
            return false;
        }

        try {
            if (!injector.inject()) {
                getLogger().error("Failed to inject the packet listener!");
                return false;
            }
        } catch (Exception exception) {
            getLogger().error("Failed to inject the packet listener!", exception);
            return false;
        }

        guice.createChildInjector(new PostInitializeModule(postCreateModules));
        return true;
    }

    public boolean disable() {
        if (injector != null) {
            try {
                if (!injector.removeInjection()) {
                    getLogger().error("Failed to remove the injection!");
                }
            } catch (Exception exception) {
                getLogger().error("Failed to remove the injection!", exception);
            }
        }

        api.getPlayerLink().stop();
        return true;
    }

    public boolean isProxy() {
        return config.isProxy();
    }
}
