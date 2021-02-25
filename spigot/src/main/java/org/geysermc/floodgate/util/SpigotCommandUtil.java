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

package org.geysermc.floodgate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudienceArgument.PlayerType;
import org.geysermc.floodgate.util.SpigotUserAudience.SpigotConsoleAudience;
import org.geysermc.floodgate.util.SpigotUserAudience.SpigotPlayerAudience;

@RequiredArgsConstructor
public final class SpigotCommandUtil implements CommandUtil {
    public static final @NonNull Map<UUID, UserAudience> AUDIENCE_CACHE = new HashMap<>();
    private static UserAudience console;

    private final Server server;
    private final FloodgateApi api;
    private final SpigotVersionSpecificMethods versionSpecificMethods;

    private final JavaPlugin plugin;
    private final FloodgateLogger logger;
    private final LanguageManager manager;

    @Override
    public @NonNull UserAudience getAudience(final @NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSender)) {
            throw new IllegalArgumentException("Source has to be a CommandSender!");
        }
        CommandSender source = (CommandSender) sourceObj;

        if (!(source instanceof Player)) {
            if (console != null) {
                return console;
            }
            return console = new SpigotConsoleAudience(source, this);
        }

        Player player = (Player) source;
        UUID uuid = player.getUniqueId();
        String locale = versionSpecificMethods.getLocale(player);

        return AUDIENCE_CACHE.computeIfAbsent(uuid,
                $ -> new SpigotPlayerAudience(uuid, locale, source, true, this));
    }

    @Override
    public @Nullable UserAudience getAudienceByUsername(@NonNull String username) {
        Player player = server.getPlayer(username);
        return player != null ? getAudience(player) : null;
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUsername(@NonNull String username) {
        return new SpigotPlayerAudience(null, username, null, null, false, this);
    }

    @Override
    public @Nullable UserAudience getAudienceByUuid(@NonNull UUID uuid) {
        Player player = server.getPlayer(uuid);
        return player != null ? getAudience(player) : null;
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUuid(@NonNull UUID uuid) {
        return new SpigotPlayerAudience(uuid, null, null, null, false, this);
    }

    @Override
    public @NonNull Collection<String> getOnlineUsernames(@NonNull PlayerType limitTo) {
        Collection<Player> players = (Collection<Player>) server.getOnlinePlayers();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (Player player : players) {
                    usernames.add(player.getName());
                }
                break;
            case ONLY_JAVA:
                for (Player player : players) {
                    if (!api.isFloodgatePlayer(player.getUniqueId())) {
                        usernames.add(player.getName());
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (Player player : players) {
                    if (api.isFloodgatePlayer(player.getUniqueId())) {
                        usernames.add(player.getName());
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown PlayerType");
        }
        return usernames;
    }

    @Override
    public void sendMessage(Object player, String locale, CommandMessage message, Object... args) {
        cast(player).sendMessage(translateAndTransform(locale, message, args));
    }

    @Override
    public void kickPlayer(Object player, String locale, CommandMessage message, Object... args) {
        // Have to run this in the main thread so we don't get a `Asynchronous player kick!` error
        Bukkit.getScheduler().runTask(plugin,
                () -> cast(player).kickPlayer(translateAndTransform(locale, message, args)));
    }

    @Override
    public boolean whitelistPlayer(String xuid, String username) {
        return WhitelistUtils.addPlayer(xuid, username);
    }

    @Override
    public boolean removePlayerFromWhitelist(String xuid, String username) {
        return WhitelistUtils.removePlayer(xuid, username);
    }

    public String translateAndTransform(String locale, CommandMessage message, Object... args) {
        // unlike others, Bukkit doesn't have to transform a message into another class.
        return message.translateMessage(manager, locale, args);
    }

    protected Player cast(Object instance) {
        try {
            return (Player) instance;
        } catch (ClassCastException exception) {
            logger.error("Failed to cast {} to Player", instance.getClass().getName());
            throw exception;
        }
    }
}
