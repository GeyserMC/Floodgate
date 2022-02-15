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

package com.minekube.connect.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.minekube.connect.addon.data.HandshakeHandlersImpl;
import com.minekube.connect.api.FloodgateApi;
import com.minekube.connect.api.SimpleFloodgateApi;
import com.minekube.connect.api.handshake.HandshakeHandlers;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.api.packet.PacketHandlers;
import com.minekube.connect.api.player.FloodgatePlayer;
import com.minekube.connect.config.FloodgateConfig;
import com.minekube.connect.config.FloodgateConfigHolder;
import com.minekube.connect.config.loader.ConfigLoader;
import com.minekube.connect.config.loader.DefaultConfigHandler;
import com.minekube.connect.config.updater.ConfigFileUpdater;
import com.minekube.connect.config.updater.ConfigUpdater;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.packet.PacketHandlersImpl;
import com.minekube.connect.util.LanguageManager;
import io.netty.util.AttributeKey;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommonModule extends AbstractModule {
    private final Path dataDirectory;

    @Override
    protected void configure() {
        bind(FloodgateApi.class).to(SimpleFloodgateApi.class);
        bind(PlatformInjector.class).to(CommonPlatformInjector.class);
        bind(HandshakeHandlers.class).to(HandshakeHandlersImpl.class);

        bind(PacketHandlers.class).to(PacketHandlersImpl.class);
        bind(PacketHandlersImpl.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("dataDirectory")
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Provides
    @Singleton
    public FloodgateConfigHolder configHolder() {
        return new FloodgateConfigHolder();
    }

    @Provides
    @Singleton
    public ConfigLoader configLoader(
            @Named("configClass") Class<? extends FloodgateConfig> configClass,
            DefaultConfigHandler defaultConfigHandler,
            ConfigUpdater configUpdater,
            FloodgateLogger logger) {
        return new ConfigLoader(dataDirectory, configClass, defaultConfigHandler, configUpdater,
                logger);
    }

    @Provides
    @Singleton
    public DefaultConfigHandler defaultConfigCreator() {
        return new DefaultConfigHandler();
    }

    @Provides
    @Singleton
    public ConfigUpdater configUpdater(
            ConfigFileUpdater configFileUpdater,
            FloodgateLogger logger) {
        return new ConfigUpdater(dataDirectory, configFileUpdater, logger);
    }

    @Provides
    @Singleton
    public LanguageManager languageLoader(
            FloodgateConfigHolder configHolder,
            FloodgateLogger logger) {
        return new LanguageManager(configHolder, logger);
    }

    @Provides
    @Singleton
    public HandshakeHandlersImpl handshakeHandlers() {
        return new HandshakeHandlersImpl();
    }
    
    @Provides
    @Singleton
    @Named("kickMessageAttribute")
    public AttributeKey<String> kickMessageAttribute() {
        return AttributeKey.valueOf("floodgate-kick-message");
    }

    @Provides
    @Singleton
    @Named("playerAttribute")
    public AttributeKey<FloodgatePlayer> playerAttribute() {
        return AttributeKey.valueOf("floodgate-player");
    }
}
