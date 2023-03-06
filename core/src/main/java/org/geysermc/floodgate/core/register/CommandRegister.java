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

package org.geysermc.floodgate.core.register;

import cloud.commandframework.CommandManager;
import jakarta.inject.Inject;
import java.util.Set;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.platform.command.FloodgateCommand;
import org.geysermc.floodgate.core.player.UserAudience;
import org.geysermc.floodgate.core.util.EagerSingleton;

/**
 * This class is responsible for registering commands to the command register of the platform that
 * is currently in use. So that the commands only have to be written once (in the common module) and
 * can be used across all platforms without the need of adding platform specific commands.
 */
@EagerSingleton
public final class CommandRegister {
    @Inject CommandManager<UserAudience> commandManager;
    @Inject FloodgateConfig config;

    @Inject
    public void registerCommands(Set<FloodgateCommand> foundCommands) {
        for (FloodgateCommand command : foundCommands) {
            if (command.shouldRegister(config)) {
                commandManager.command(command.buildCommand(commandManager));
            }
        }
    }
}
