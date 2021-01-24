/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import cloud.commandframework.CommandManager;
import cloud.commandframework.bungee.BungeeCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
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
import org.geysermc.floodgate.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.BungeeSkinApplier;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.BungeeCommandUtil;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class BungeePlatformModule extends AbstractModule {
    private final BungeePlugin plugin;

    @Provides
    @Singleton
    public Plugin bungeePlugin() {
        return plugin;
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
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        CommandManager<UserAudience> commandManager = new BungeeCommandManager<>(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getAudience,
                audience -> (CommandSender) audience.source()
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new BungeeCommandUtil(plugin.getProxy(), api, logger, languageManager);
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

    @Provides
    @Singleton
    public SkinApplier skinApplier(FloodgateLogger logger) {
        return new BungeeSkinApplier(logger);
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
