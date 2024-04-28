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
import com.google.inject.name.Names;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.BungeePlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.bungee.BungeeInjector;
import org.geysermc.floodgate.listener.BungeeListenerRegistration;
import org.geysermc.floodgate.logger.JavaUtilFloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.platform.util.PlatformUtils;
import org.geysermc.floodgate.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.audience.FloodgateSenderMapper;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.BungeeSkinApplier;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.BungeeCommandUtil;
import org.geysermc.floodgate.util.BungeePlatformUtils;
import org.geysermc.floodgate.util.LanguageManager;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bungee.BungeeCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

@RequiredArgsConstructor
public final class BungeePlatformModule extends AbstractModule {
    private final BungeePlugin plugin;

    @Override
    protected void configure() {
        bind(PlatformUtils.class).to(BungeePlatformUtils.class);
        bind(Logger.class).annotatedWith(Names.named("logger")).toInstance(plugin.getLogger());
        bind(FloodgateLogger.class).to(JavaUtilFloodgateLogger.class);
        bind(SkinApplier.class).to(BungeeSkinApplier.class);
    }

    @Provides
    @Singleton
    public Plugin bungeePlugin() {
        return plugin;
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        CommandManager<UserAudience> commandManager = new BungeeCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                new FloodgateSenderMapper<>(commandUtil)
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(FloodgateApi api, LanguageManager languageManager) {
        return new BungeeCommandUtil(languageManager, plugin.getProxy(), api);
    }

    @Provides
    @Singleton
    public ListenerRegistration<Listener> listenerRegistration() {
        return new BungeeListenerRegistration(plugin);
    }

    @Provides
    @Singleton
    public PluginMessageUtils pluginMessageUtils(
            PluginMessageManager manager,
            FloodgateLogger logger) {
        return new BungeePluginMessageUtils(manager, logger);
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegistration() {
        return new BungeePluginMessageRegistration();
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
