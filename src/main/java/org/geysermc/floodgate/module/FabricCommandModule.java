package org.geysermc.floodgate.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.audience.FloodgateSenderMapper;
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
        return commandManager;
    }

}
