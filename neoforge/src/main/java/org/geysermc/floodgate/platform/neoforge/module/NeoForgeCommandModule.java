package org.geysermc.floodgate.platform.neoforge.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import org.geysermc.floodgate.core.module.CommandModule;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.core.player.UserAudience;
import org.geysermc.floodgate.core.player.audience.FloodgateSenderMapper;
import org.geysermc.floodgate.mod.util.ModCommandUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.neoforge.NeoForgeServerCommandManager;

public class NeoForgeCommandModule extends CommandModule {
    @Provides
    @Singleton
    @SneakyThrows
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        CommandManager<UserAudience> commandManager = new NeoForgeServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                new FloodgateSenderMapper<>(commandUtil)
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        ((ModCommandUtil) commandUtil).setCommandManager(commandManager);
        return commandManager;
    }
}
