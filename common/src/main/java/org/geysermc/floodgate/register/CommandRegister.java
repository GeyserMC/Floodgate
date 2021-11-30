/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.register;

import cloud.commandframework.CommandManager;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.Set;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.player.UserAudience;

/**
 * This class is responsible for registering commands to the command register of the platform that
 * is currently in use. So that the commands only have to be written once (in the common module) and
 * can be used across all platforms without the need of adding platform specific commands.
 */
public final class CommandRegister {
    private final CommandManager<UserAudience> commandManager;
    private final FloodgateConfig config;
    private final Injector guice;

    @Inject
    public CommandRegister(Injector guice) {
        this.commandManager = guice.getInstance(new Key<CommandManager<UserAudience>>() {});
        this.config = guice.getInstance(FloodgateConfig.class);
        this.guice = guice;
    }

    @Inject
    public void registerCommands(Set<FloodgateCommand> foundCommands) {
        for (FloodgateCommand command : foundCommands) {
            guice.injectMembers(command);
            if (command.shouldRegister(config)) {
                commandManager.command(command.buildCommand(commandManager));
            }
        }
    }
}
