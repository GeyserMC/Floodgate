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

package org.geysermc.floodgate.platform.command;

import java.util.Collection;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudienceArgument.PlayerType;
import org.geysermc.floodgate.util.Utils;

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

    @NonNull Collection<String> getOnlineUsernames(final @NonNull PlayerType limitTo);

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

    /**
     * Whitelist the given Bedrock player.
     *
     * @param xuid     the xuid of the username to be whitelisted
     * @param username the username to be whitelisted
     * @return true if the player has been whitelisted, false if the player was already whitelisted.
     * Defaults to false when this platform doesn't support whitelisting.
     */
    default boolean whitelistPlayer(String xuid, String username) {
        UUID uuid = Utils.getJavaUuid(xuid);
        return whitelistPlayer(uuid, username);
    }

    /**
     * Whitelist the given Bedrock player.
     *
     * @param uuid     the UUID of the username to be whitelisted
     * @param username the username to be whitelisted
     * @return true if the player has been whitelisted, false if the player was already whitelisted.
     * Defaults to false when this platform doesn't support whitelisting.
     */
    default boolean whitelistPlayer(UUID uuid, String username) {
        return false;
    }

    /**
     * Removes the given Bedrock player from the whitelist.
     *
     * @param xuid     the xuid of the username to be removed from the whitelist
     * @param username the username to be removed from the whitelist
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     * whitelisted. Defaults to false when this platform doesn't support whitelisting.
     */
    default boolean removePlayerFromWhitelist(String xuid, String username) {
        UUID uuid = Utils.getJavaUuid(xuid);
        return removePlayerFromWhitelist(uuid, username);
    }

    /**
     * Removes the given Bedrock player from the whitelist.
     *
     * @param uuid     the UUID of the username to be removed from the whitelist
     * @param username the username to be removed from the whitelist
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     * whitelisted. Defaults to false when this platform doesn't support whitelisting.
     */
    default boolean removePlayerFromWhitelist(UUID uuid, String username) {
        return false;
    }
}
