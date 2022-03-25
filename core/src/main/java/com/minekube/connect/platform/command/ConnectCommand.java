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

package com.minekube.connect.platform.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.player.UserAudience;

/**
 * The base class for every Connect command.
 */
public interface ConnectCommand {
    /**
     * Called by the CommandRegister when it wants you to build the command which he can add.
     *
     * @param commandManager the manager to create a command
     * @return the command to register
     */
    Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager);

    /**
     * Called when the command created in {@link #buildCommand(CommandManager)} is executed.
     *
     * @param context the context of the executed command
     */
    void execute(CommandContext<UserAudience> context);

    /**
     * Called by the CommandRegister to check if the command should be added given the config.
     *
     * @param config the config to check if a command should be added
     * @return true if it should be added
     */
    default boolean shouldRegister(ConnectConfig config) {
        return true;
    }
}
