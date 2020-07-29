/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import org.geysermc.floodgate.platform.command.util.CommandUtil;

import java.util.UUID;

/**
 * The base class for every Floodgate command.
 */
public interface Command {
    /**
     * Should be implemented when {@link #isRequirePlayer()} is true
     * or when the source is a player.
     *
     * @param player   the player instance (used for example in combination with
     *                 {@link CommandUtil#kickPlayer(Object, CommandMessage, Object...)}
     * @param uuid     the uuid of the player
     * @param username the username of the player
     * @param args     the arguments of the command
     */
    default void execute(Object player, UUID uuid, String username, String... args) {
    }

    /**
     * Should be implemented when {@link #isRequirePlayer()} is false.
     *
     * @param source the CommandSource (Velocity) or CommandExecutor (Bungee and Bukkit) that
     *               executed this command
     * @param args   the arguments of the command
     */
    default void execute(Object source, String... args) {
        if (isRequirePlayer()) {
            throw new RuntimeException(
                    "Cannot execute this command since it requires a player"
            );
        }
    }

    /**
     * The command name that should be registered and used by the CommandSource.
     *
     * @return the name of the command that should be registered
     */
    String getName();

    /**
     * The permission that is required to execute the specific command.
     * Should return null when there is no permission required.
     *
     * @return the permission required to execute the command
     */
    String getPermission();

    /**
     * If the Command requires a Player to execute this command
     * or if it doesn't matter if (for example) the console executes the command.
     *
     * @return true if this command can only be executed by a player
     */
    boolean isRequirePlayer();
}
