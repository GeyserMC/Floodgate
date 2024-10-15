/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.spigot.command;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.connection.audience.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.core.connection.audience.FloodgateSenderMapper;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

@Factory
public class SpigotCommandManager {
    @Bean
    @SneakyThrows
    @Singleton
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil, JavaPlugin plugin) {
        var commandManager = new LegacyPaperCommandManager<>(
                plugin, ExecutionCoordinator.simpleCoordinator(), new FloodgateSenderMapper<>(commandUtil));
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

    @Inject
    void registerPermissions() {
        PluginManager manager = Bukkit.getPluginManager();
        for (Permission permission : Permission.values()) {
            if (manager.getPermission(permission.get()) != null) {
                continue;
            }

            PermissionDefault defaultValue =
                    PermissionDefault.getByName(permission.defaultValue().name());

            manager.addPermission(new org.bukkit.permissions.Permission(permission.get(), defaultValue));
        }
    }
}
