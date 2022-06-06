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

import static org.geysermc.floodgate.platform.util.PlayerType.ALL_PLAYERS;
import static org.geysermc.floodgate.platform.util.PlayerType.ONLY_BEDROCK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.platform.util.PlayerType;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.audience.ProfileAudience;
import org.geysermc.floodgate.util.LanguageManager;
import org.geysermc.floodgate.util.Utils;

/**
 * An interface used across all Floodgate platforms to simple stuff in commands like kicking players
 * and sending player messages independent of the Floodgate platform implementation.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CommandUtil {
    protected final LanguageManager manager;
    protected final FloodgateApi api;

    public abstract @NonNull UserAudience getUserAudience(@NonNull Object source);

    /**
     * Get a ProfileAudience from a source. The source should be a platform-specific player instance
     * when the player is online, and the username / uuid of the requested player when offline.
     *
     * @param source       source to create a ProfileAudience from
     * @param allowOffline if offline players are allowed
     * @return a ProfileAudience unless allowOffline is false and the player isn't online
     */
    public @Nullable ProfileAudience getProfileAudience(
            @NonNull Object source,
            boolean allowOffline) {
        Objects.requireNonNull(source);

        if (source instanceof UUID) {
            return allowOffline ? new ProfileAudience((UUID) source, null) : null;
        } else if (source instanceof String) {
            return allowOffline ? new ProfileAudience(null, (String) source) : null;
        } else {
            return new ProfileAudience(getUuidFromSource(source), getUsernameFromSource(source));
        }
    }

    protected abstract String getUsernameFromSource(@NonNull Object source);
    protected abstract UUID getUuidFromSource(@NonNull Object source);

    protected abstract Collection<?> getOnlinePlayers();

    public @NonNull Collection<String> getOnlineUsernames(@NonNull PlayerType limitTo) {
        Collection<?> players = getOnlinePlayers();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (Object player : players) {
                    usernames.add(getUsernameFromSource(player));
                }
                break;
            case ONLY_JAVA:
                for (Object player : players) {
                    if (!api.isFloodgatePlayer(getUuidFromSource(player))) {
                        usernames.add(getUsernameFromSource(player));
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (Object player : players) {
                    if (api.isFloodgatePlayer(getUuidFromSource(player))) {
                        usernames.add(getUsernameFromSource(player));
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown PlayerType");
        }
        return usernames;
    }

    /**
     *
     * @param uuid
     * @return
     */
    public abstract Object getPlayerByUuid(@NonNull UUID uuid);

    public Object getPlayerByUuid(@NonNull UUID uuid, PlayerType limitTo) {
        return applyPlayerTypeFilter(getPlayerByUuid(uuid), limitTo, uuid);
    }

    public abstract Object getPlayerByUsername(@NonNull String username);

    public Object getPlayerByUsername(@NonNull String username, PlayerType limitTo) {
        return applyPlayerTypeFilter(getPlayerByUsername(username), limitTo, username);
    }

    protected Object applyPlayerTypeFilter(Object player, PlayerType filter, Object fallback) {
        if (filter == ALL_PLAYERS || player instanceof String || player instanceof UUID) {
            return player;
        }
        return (filter == ONLY_BEDROCK) == api.isFloodgatePlayer(getUuidFromSource(player))
                ? player
                : fallback;
    }

    /**
     * Checks if the given player has the given permission.
     *
     * @param player     the player to check
     * @param permission the permission to check
     * @return true or false depending on if the player has the permission
     */
    public abstract boolean hasPermission(Object player, String permission);

    /**
     * Get all online players with the given permission.
     *
     * @param permission the permission to check
     * @return a list of online players that have the given permission
     */
    public Collection<Object> getOnlinePlayersWithPermission(String permission) {
        List<Object> players = new ArrayList<>();
        for (Object player : getOnlinePlayers()) {
            if (hasPermission(player, permission)) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Sends a raw message to the specified target, no matter what platform Floodgate is running
     * on.
     *
     * @param target  the player that should receive the message
     * @param message the message
     */
    public abstract void sendMessage(Object target, String message);

    /**
     * Kicks the given player using the given message as the kick reason.
     *
     * @param player  the player that should be kicked
     * @param message the command message
     */
    public abstract void kickPlayer(Object player, String message);

    public String translateMessage(String locale, TranslatableMessage message, Object... args) {
        return message.translateMessage(manager, locale, args);
    }

    /**
     * Whitelist the given Bedrock player.
     *
     * @param xuid     the xuid of the username to be whitelisted
     * @param username the username to be whitelisted
     * @return true if the player has been whitelisted, false if the player was already whitelisted.
     * Defaults to false when this platform doesn't support whitelisting.
     */
    public boolean whitelistPlayer(String xuid, String username) {
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
    public boolean whitelistPlayer(UUID uuid, String username) {
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
    public boolean removePlayerFromWhitelist(String xuid, String username) {
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
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        return false;
    }
}
