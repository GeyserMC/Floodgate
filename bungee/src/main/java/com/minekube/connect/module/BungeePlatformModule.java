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

import cloud.commandframework.CommandManager;
import cloud.commandframework.bungee.BungeeCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.minekube.connect.BungeePlugin;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.inject.bungee.BungeeInjector;
import com.minekube.connect.listener.BungeeListenerRegistration;
import com.minekube.connect.logger.JavaUtilConnectLogger;
import com.minekube.connect.platform.command.CommandUtil;
import com.minekube.connect.platform.listener.ListenerRegistration;
import com.minekube.connect.player.ConnectCommandPreprocessor;
import com.minekube.connect.player.UserAudience;
import com.minekube.connect.pluginmessage.BungeeSkinApplier;
import com.minekube.connect.skin.SkinApplier;
import com.minekube.connect.util.BungeeCommandUtil;
import com.minekube.connect.util.LanguageManager;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

@RequiredArgsConstructor
public final class BungeePlatformModule extends AbstractModule {
    private final BungeePlugin plugin;

    @Override
    protected void configure() {
        bind(ProxyServer.class).toInstance(plugin.getProxy());
    }

    @Provides
    @Singleton
    public Plugin bungeePlugin() {
        return plugin;
    }

    @Provides
    @Singleton
    public ConnectLogger logger(LanguageManager languageManager) {
        return new JavaUtilConnectLogger(plugin.getLogger(), languageManager);
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        CommandManager<UserAudience> commandManager = new BungeeCommandManager<>(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getUserAudience,
                audience -> (CommandSender) audience.source()
        );
        commandManager.registerCommandPreProcessor(new ConnectCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(ConnectApi api, LanguageManager languageManager) {
        return new BungeeCommandUtil(languageManager, plugin.getProxy(), api);
    }

    @Provides
    @Singleton
    public ListenerRegistration<Listener> listenerRegistration() {
        return new BungeeListenerRegistration(plugin);
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier(ConnectLogger logger) {
        return new BungeeSkinApplier(logger);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector(ConnectLogger logger, ProxyServer proxy,
                                                   Plugin plugin) {
        return new BungeeInjector(logger, proxy, plugin);
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
        return "BungeeCord";
    }
}
