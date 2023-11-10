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

package org.geysermc.floodgate.bungee.module;

import cloud.commandframework.CommandManager;
import cloud.commandframework.bungee.BungeeCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.core.connection.audience.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.platform.command.CommandUtil;

@Factory
public final class BungeePlatformModule {
    @Bean
    @Singleton
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil, Plugin plugin) {
        CommandManager<UserAudience> commandManager = new BungeeCommandManager<>(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getUserAudience,
                audience -> (CommandSender) audience.source()
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Bean
    @Named("packetEncoder")
    @Singleton
    public String packetEncoder() {
        return "packet-encoder";
    }

    @Bean
    @Named("packetDecoder")
    @Singleton
    public String packetDecoder() {
        return "packet-decoder";
    }

    @Bean
    @Named("packetHandler")
    @Singleton
    public String packetHandler() {
        return "inbound-boss";
    }

    @Bean
    @Named("implementationName")
    @Singleton
    public String implementationName() {
        return "Bungeecord";
    }
}
