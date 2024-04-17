/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.platform.command;

import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.UserAudience;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

/** The base class for every Floodgate command. */
public interface FloodgateCommand {
    /**
     * Called by the CommandRegister when it wants you to build the command which he can add.
     *
     * @param commandManager the manager to create a command
     * @return the command to register
     */
    Command<? extends UserAudience> buildCommand(CommandManager<UserAudience> commandManager);

    /**
     * Called by the CommandRegister to check if the command should be added given the config.
     *
     * @param config the config to check if a command should be added
     * @return true if it should be added
     */
    default boolean shouldRegister(FloodgateConfig config) {
      return true;
    }
}
