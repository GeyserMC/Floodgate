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

import com.minekube.connect.player.UserAudience;
import com.minekube.connect.player.UserAudienceArgument;
import java.util.Collection;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An interface used across all Floodgate platforms to simple stuff in commands like kicking players
 * and sending player messages independent of the Floodgate platform implementation.
 */
public interface CommandUtil {
    @NonNull UserAudience getAudience(final @NonNull Object source);

    @Nullable UserAudience getAudienceByUuid(final @NonNull UUID uuid);

    @NonNull UserAudience getOfflineAudienceByUuid(final @NonNull UUID uuid);

    @Nullable UserAudience getAudienceByUsername(final @NonNull String username);

    @NonNull UserAudience getOfflineAudienceByUsername(final @NonNull String username);

    @NonNull Collection<String> getOnlineUsernames(
            final UserAudienceArgument.@NonNull PlayerType limitTo);

    /**
     * Checks if the given player has the given permission.
     *
     * @param player     the player to check
     * @param permission the permission to check
     * @return true or false depending on if the player has the permission
     */
    boolean hasPermission(Object player, String permission);

    /**
     * Get all online players with the given permission.
     *
     * @param permission the permission to check
     * @return a list of online players that have the given permission
     */
    Collection<Object> getOnlinePlayersWithPermission(String permission);

    /**
     * Send a message to the specified target, no matter what platform Floodgate is running on.
     *
     * @param target  the player that should receive the message
     * @param message the command message
     * @param locale  the locale of the player
     * @param args    the arguments
     */
    void sendMessage(Object target, String locale, TranslatableMessage message, Object... args);

    /**
     * Sends a raw message to the specified target, no matter what platform Floodgate is running
     * on.
     *
     * @param target  the player that should receive the message
     * @param message the message
     */
    void sendMessage(Object target, String message);

    /**
     * Same as {@link CommandUtil#sendMessage(Object, String, TranslatableMessage, Object...)}
     * except it kicks the player using the given message as the kick reason.
     *
     * @param player  the player that should be kicked
     * @param message the command message
     * @param locale  the locale of the player
     * @param args    the arguments
     */
    void kickPlayer(Object player, String locale, TranslatableMessage message, Object... args);
}
