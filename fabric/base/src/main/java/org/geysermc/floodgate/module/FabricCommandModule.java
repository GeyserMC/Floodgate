package org.geysermc.floodgate.module;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricCommandManager;
import cloud.commandframework.fabric.FabricServerCommandManager;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.FloodgateCommandPreprocessor;
import org.geysermc.floodgate.player.UserAudience;

public final class FabricCommandModule extends CommandModule {
    @Provides
    @Singleton
    @SneakyThrows
    public CommandManager<UserAudience> commandManager(CommandUtil commandUtil) {
        FabricCommandManager<UserAudience, CommandSourceStack> commandManager = new FabricServerCommandManager<>(
                CommandExecutionCoordinator.simpleCoordinator(),
                commandUtil::getUserAudience,
                audience -> (CommandSourceStack) audience.source()
        );
        commandManager.registerCommandPreProcessor(new FloodgateCommandPreprocessor<>(commandUtil));
        return commandManager;
    }

}
