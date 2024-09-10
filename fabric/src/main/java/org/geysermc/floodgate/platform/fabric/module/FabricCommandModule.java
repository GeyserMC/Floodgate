package org.geysermc.floodgate.platform.fabric.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import org.geysermc.floodgate.core.module.CommandModule;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.core.player.UserAudience;
import org.geysermc.floodgate.core.player.audience.FloodgateSenderMapper;
import org.geysermc.floodgate.mod.util.ModCommandUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricCommandManager;
import org.incendo.cloud.fabric.FabricServerCommandManager;

public final class FabricCommandModule extends CommandModule {
    @Provides
    @Singleton
    @SneakyThrows
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        FabricCommandManager<UserAudience, CommandSourceStack> commandManager = new FabricServerCommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                new FloodgateSenderMapper<>(commandUtil)
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        ((ModCommandUtil) commandUtil).setCommandManager(commandManager);
        return commandManager;
    }

}
