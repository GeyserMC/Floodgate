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

package org.geysermc.floodgate.fabric.module;

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
