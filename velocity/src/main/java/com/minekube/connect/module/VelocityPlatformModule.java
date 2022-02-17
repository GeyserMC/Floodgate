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
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.CloudInjectionModule;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.minekube.connect.VelocityPlugin;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.inject.velocity.FloodgateVelocityInjector;
import com.minekube.connect.listener.VelocityEventSink;
import com.minekube.connect.listener.VelocityListenerRegistration;
import com.minekube.connect.logger.Slf4jFloodgateLogger;
import com.minekube.connect.platform.command.CommandUtil;
import com.minekube.connect.platform.listener.EventSink;
import com.minekube.connect.platform.listener.ListenerRegistration;
import com.minekube.connect.player.FloodgateCommandPreprocessor;
import com.minekube.connect.player.UserAudience;
import com.minekube.connect.skin.SkinApplier;
import com.minekube.connect.util.LanguageManager;
import com.minekube.connect.util.VelocityCommandUtil;
import com.minekube.connect.util.VelocitySkinApplier;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

@RequiredArgsConstructor
public final class VelocityPlatformModule extends AbstractModule {
    private final Injector guice;

    @Override
    protected void configure() {
        VelocityCommandUtil commandUtil = new VelocityCommandUtil();
        requestInjection(commandUtil);

        bind(CommandUtil.class).to(VelocityCommandUtil.class);
        bind(VelocityCommandUtil.class).toInstance(commandUtil);

        Injector child = guice.createChildInjector(new CloudInjectionModule<>(
                UserAudience.class,
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getAudience,
                audience -> (CommandSource) audience.source()
        ));

        CommandManager<UserAudience> commandManager =
                child.getInstance(new Key<VelocityCommandManager<UserAudience>>() {});

        bind(new Key<CommandManager<UserAudience>>() {}).toInstance(commandManager);

        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
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
    public ListenerRegistration<Object> listenerRegistration(EventManager eventManager,
                                                             VelocityPlugin plugin) {
        return new VelocityListenerRegistration(eventManager, plugin);
    }

    @Provides
    @Singleton
    public EventSink eventSink(EventManager eventManager) {
        return new VelocityEventSink(eventManager);
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier(ProxyServer server) {
        return new VelocitySkinApplier(server);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector(ProxyServer server, FloodgateLogger logger) {
        return new FloodgateVelocityInjector(server, logger);
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
