/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.util.AttributeKey;
import org.geysermc.floodgate.VelocityPlugin;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.command.VelocityCommandRegistration;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.velocity.VelocityInjector;
import org.geysermc.floodgate.listener.VelocityListenerRegistration;
import org.geysermc.floodgate.listener.VelocityPluginMessageHandler;
import org.geysermc.floodgate.logger.Slf4jFloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.util.LanguageManager;
import org.geysermc.floodgate.util.VelocityCommandUtil;
import org.slf4j.Logger;

public final class VelocityPlatformModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SimpleFloodgateApi.class).to(ProxyFloodgateApi.class);
        bind(CommandUtil.class).to(VelocityCommandUtil.class);
    }

    @Provides
    @Singleton
    @Named("configClass")
    public Class<? extends FloodgateConfig> floodgateConfigClass() {
        return ProxyFloodgateConfig.class;
    }

    @Provides
    @Singleton
    public ProxyFloodgateApi proxyFloodgateApi(PluginMessageHandler pluginMessageHandler,
                                               FloodgateCipher cipher) {
        return new ProxyFloodgateApi(pluginMessageHandler, cipher);
    }

    @Provides
    @Singleton
    public FloodgateLogger floodgateLogger(Logger logger, LanguageManager languageManager) {
        return new Slf4jFloodgateLogger(logger, languageManager);
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandRegistration commandRegistration(CommandManager commandManager,
                                                   VelocityCommandUtil commandUtil,
                                                   LanguageManager languageManager) {
        return new VelocityCommandRegistration(commandManager, commandUtil, languageManager);
    }

    @Provides
    @Singleton
    public VelocityCommandUtil commandUtil(FloodgateLogger logger,
                                           LanguageManager languageManager) {
        return new VelocityCommandUtil(logger, languageManager);
    }

    @Provides
    @Singleton
    public ListenerRegistration<Object> listenerRegistration(EventManager eventManager,
                                                             VelocityPlugin plugin) {
        return new VelocityListenerRegistration(eventManager, plugin);
    }

    @Provides
    @Singleton
    public PluginMessageHandler pluginMessageHandler(FloodgateConfigHolder configHolder) {
        return new VelocityPluginMessageHandler(configHolder);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector(ProxyServer server) {
        return new VelocityInjector(server);
    }

    @Provides
    @Named("packetEncoder")
    public String packetEncoder() {
        return "minecraft-encoder";
    }

    @Provides
    @Named("packetDecoder")
    public String packetDecoder() {
        return "minecraft-decoder";
    }

    @Provides
    @Named("packetHandler")
    public String packetHandler() {
        return "handler";
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Velocity";
    }

    @Provides
    @Singleton
    @Named("kickMessageAttribute")
    public AttributeKey<String> kickMessageAttribute() {
        return AttributeKey.valueOf("floodgate-kick-message");
    }
}
