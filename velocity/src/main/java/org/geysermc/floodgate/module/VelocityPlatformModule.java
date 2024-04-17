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
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.VelocityPlugin;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.velocity.VelocityInjector;
import org.geysermc.floodgate.listener.VelocityListenerRegistration;
import org.geysermc.floodgate.logger.Slf4jFloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.platform.util.PlatformUtils;
import org.geysermc.floodgate.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.audience.FloodgateSenderMapper;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.VelocityPluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.VelocityPluginMessageUtils;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.VelocityCommandUtil;
import org.geysermc.floodgate.util.VelocityPlatformUtils;
import org.geysermc.floodgate.util.VelocitySkinApplier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.CloudInjectionModule;
import org.incendo.cloud.velocity.VelocityCommandManager;

@RequiredArgsConstructor
public final class VelocityPlatformModule extends AbstractModule {
    private final Injector guice;

    @Override
    protected void configure() {
        bind(CommandUtil.class).to(VelocityCommandUtil.class);
        bind(PlatformUtils.class).to(VelocityPlatformUtils.class);
        bind(FloodgateLogger.class).to(Slf4jFloodgateLogger.class);
        bind(SkinApplier.class).to(VelocitySkinApplier.class);
    }

    @Provides
    @Singleton
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        Injector child = guice.createChildInjector(new CloudInjectionModule<>(
                UserAudience.class,
                ExecutionCoordinator.simpleCoordinator(),
                new FloodgateSenderMapper<>(commandUtil)
        ));

        CommandManager<UserAudience> commandManager =
                child.getInstance(new Key<VelocityCommandManager<UserAudience>>() {});

        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public ListenerRegistration<Object> listenerRegistration(
            EventManager eventManager,
            VelocityPlugin plugin) {
        return new VelocityListenerRegistration(eventManager, plugin);
    }

    @Provides
    @Singleton
    public PluginMessageUtils pluginMessageUtils(PluginMessageManager pluginMessageManager) {
        return new VelocityPluginMessageUtils(pluginMessageManager);
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegistration(ProxyServer proxy) {
        return new VelocityPluginMessageRegistration(proxy);
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
}
