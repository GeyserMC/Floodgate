/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.velocity.module;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.connection.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.core.connection.UserAudience;
import org.geysermc.floodgate.core.platform.command.CommandUtil;

@Factory
public class VelocityPlatformModule {
    @Bean
    @Singleton
    public CommandManager<UserAudience> commandManager(
            CommandUtil commandUtil,
            ProxyServer proxy,
            PluginContainer container
    ) {
        CommandManager<UserAudience> commandManager = new VelocityCommandManager<>(
                container,
                proxy,
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getUserAudience,
                audience -> (CommandSource) audience.source()
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Bean
    @Named("packetEncoder")
    @Singleton
    public String packetEncoder() {
        return "minecraft-encoder";
    }

    @Bean
    @Named("packetDecoder")
    @Singleton
    public String packetDecoder() {
        return "minecraft-decoder";
    }

    @Bean
    @Named("packetHandler")
    @Singleton
    public String packetHandler() {
        return "handler";
    }

    @Bean
    @Named("implementationName")
    @Singleton
    public String implementationName() {
        return "Velocity";
    }
}
