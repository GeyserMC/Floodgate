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

package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.util.AttributeKey;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.addon.data.HandshakeHandlersImpl;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.config.loader.ConfigLoader;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.crypto.KeyProducer;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.news.NewsChecker;
import org.geysermc.floodgate.packet.PacketHandlersImpl;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinUploadManager;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.LanguageManager;

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
    public KeyProducer keyProducer() {
        return new AesKeyProducer();
    }

    @Provides
    @Singleton
    public FloodgateCipher cipher() {
        return new AesCipher(new Base64Topping());
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
            KeyProducer producer,
            FloodgateCipher cipher,
            FloodgateLogger logger) {
        return new ConfigLoader(dataDirectory, configClass, producer, cipher, logger);
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
    public FloodgateHandshakeHandler handshakeHandler(
            HandshakeHandlersImpl handshakeHandlers,
            SimpleFloodgateApi api,
            FloodgateCipher cipher,
            FloodgateConfigHolder configHolder,
            SkinUploadManager skinUploadManager,
            @Named("playerAttribute") AttributeKey<FloodgatePlayer> playerAttribute,
            FloodgateLogger logger) {

        return new FloodgateHandshakeHandler(handshakeHandlers, api, cipher, configHolder,
                skinUploadManager, playerAttribute, logger);
    }

    @Provides
    @Singleton
    public PluginMessageManager pluginMessageManager() {
        return new PluginMessageManager();
    }

    @Provides
    @Singleton
    public SkinUploadManager skinUploadManager(
            FloodgateApi api,
            SkinApplier skinApplier,
            FloodgateLogger logger) {
        return new SkinUploadManager(api, skinApplier, logger);
    }

    @Provides
    @Singleton
    public NewsChecker newsChecker(CommandUtil commandUtil, FloodgateLogger logger) {
        return new NewsChecker(commandUtil, logger, Constants.GIT_BRANCH, Constants.BUILD_NUMBER);
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
