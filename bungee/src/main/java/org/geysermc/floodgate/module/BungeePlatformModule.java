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
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.BungeePlugin;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.command.BungeeCommandRegistration;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.bungee.BungeeInjector;
import org.geysermc.floodgate.listener.BungeeListenerRegistration;
import org.geysermc.floodgate.logger.JavaUtilFloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageHandler;
import org.geysermc.floodgate.util.BungeeCommandUtil;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class BungeePlatformModule extends AbstractModule {
    private final BungeePlugin plugin;

    @Override
    protected void configure() {
        bind(SimpleFloodgateApi.class).to(ProxyFloodgateApi.class);
    }

    @Provides
    @Singleton
    public Plugin bungeePlugin() {
        return plugin;
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
    public FloodgateLogger floodgateLogger(LanguageManager languageManager) {
        return new JavaUtilFloodgateLogger(plugin.getLogger(), languageManager);
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandRegistration commandRegistration(CommandUtil commandUtil,
                                                   LanguageManager languageManager) {
        return new BungeeCommandRegistration(plugin, commandUtil, languageManager);
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(FloodgateLogger logger, LanguageManager languageManager) {
        return new BungeeCommandUtil(logger, languageManager);
    }

    @Provides
    @Singleton
    public ListenerRegistration<Listener> listenerRegistration() {
        return new BungeeListenerRegistration(plugin);
    }

    @Provides
    @Singleton
    public PluginMessageHandler pluginMessageHandler(FloodgateConfigHolder configHolder) {
        return new BungeePluginMessageHandler(configHolder);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector(FloodgateLogger logger) {
        return new BungeeInjector(logger);
    }

    @Provides
    @Named("packetEncoder")
    public String packetEncoder() {
        return "packet-encoder";
    }

    @Provides
    @Named("packetDecoder")
    public String packetDecoder() {
        return "packet-decoder";
    }

    @Provides
    @Named("packetHandler")
    public String packetHandler() {
        return "inbound-boss";
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Bungeecord";
    }
}
