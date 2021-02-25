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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudienceArgument.PlayerType;
import org.geysermc.floodgate.util.BungeeUserAudience.BungeeConsoleAudience;
import org.geysermc.floodgate.util.BungeeUserAudience.BungeePlayerAudience;

@RequiredArgsConstructor
public final class BungeeCommandUtil implements CommandUtil {
    public static final @NonNull Map<UUID, UserAudience> AUDIENCE_CACHE = new HashMap<>();
    private static UserAudience console;

    private final ProxyServer server;
    private final FloodgateApi api;

    private final FloodgateLogger logger;
    private final LanguageManager manager;

    @Override
    public @NonNull UserAudience getAudience(@NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSender)) {
            throw new IllegalArgumentException("Can only work with CommandSource!");
        }
        CommandSender source = (CommandSender) sourceObj;

        if (!(source instanceof ProxiedPlayer)) {
            if (console != null) {
                return console;
            }
            return console = new BungeeConsoleAudience(source, this);
        }

        ProxiedPlayer player = (ProxiedPlayer) source;
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        String locale = Utils.getLocale(player.getLocale());

        return AUDIENCE_CACHE.computeIfAbsent(uuid,
                $ -> new BungeePlayerAudience(uuid, username, locale, source, true, this));
    }

    @Override
    public @Nullable UserAudience getAudienceByUsername(@NonNull String username) {
        ProxiedPlayer player = server.getPlayer(username);
        return player != null ? getAudience(player) : null;
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUsername(@NonNull String username) {
        return new BungeePlayerAudience(null, username, null, null, false, this);
    }

    @Override
    public @Nullable UserAudience getAudienceByUuid(@NonNull UUID uuid) {
        ProxiedPlayer player = server.getPlayer(uuid);
        return player != null ? getAudience(player) : null;
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUuid(@NonNull UUID uuid) {
        return new BungeePlayerAudience(uuid, null, null, null, false, this);
    }

    @Override
    public @NonNull Collection<String> getOnlineUsernames(@NonNull PlayerType limitTo) {
        Collection<ProxiedPlayer> players = server.getPlayers();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (ProxiedPlayer player : players) {
                    usernames.add(player.getName());
                }
                break;
            case ONLY_JAVA:
                for (ProxiedPlayer player : players) {
                    if (!api.isFloodgatePlayer(player.getUniqueId())) {
                        usernames.add(player.getName());
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (ProxiedPlayer player : players) {
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
        cast(player).disconnect(translateAndTransform(locale, message, args));
    }

    @Override
    public boolean whitelistPlayer(String xuid, String username) {
        return false; // todo
    }

    @Override
    public boolean removePlayerFromWhitelist(String xuid, String username) {
        return false; // todo
    }

    public BaseComponent[] translateAndTransform(String locale, CommandMessage message,
                                                 Object... args) {
        return TextComponent.fromLegacyText(message.translateMessage(manager, locale, args));
    }

    protected ProxiedPlayer cast(Object player) {
        try {
            return (ProxiedPlayer) player;
        } catch (ClassCastException exception) {
            logger.error("Failed to cast {} to ProxiedPlayer", player.getClass().getName());
            throw exception;
        }
    }
}
