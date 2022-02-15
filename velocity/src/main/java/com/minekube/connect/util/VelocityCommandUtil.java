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

package com.minekube.connect.util;

import com.google.inject.Inject;
import com.minekube.connect.api.FloodgateApi;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.platform.command.CommandUtil;
import com.minekube.connect.platform.command.TranslatableMessage;
import com.minekube.connect.player.UserAudience;
import com.minekube.connect.player.UserAudienceArgument.PlayerType;
import com.minekube.connect.player.VelocityUserAudience.VelocityConsoleAudience;
import com.minekube.connect.player.VelocityUserAudience.VelocityPlayerAudience;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class VelocityCommandUtil implements CommandUtil {
    public static final @NonNull Map<UUID, UserAudience> AUDIENCE_CACHE = new HashMap<>();
    private static UserAudience console;

    @Inject private ProxyServer server;
    @Inject private FloodgateApi api;
    @Inject private FloodgateLogger logger;
    @Inject private LanguageManager manager;

    @Override
    public @NonNull UserAudience getAudience(@NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSource)) {
            throw new IllegalArgumentException("Can only work with CommandSource!");
        }
        CommandSource source = (CommandSource) sourceObj;

        if (!(source instanceof Player)) {
            if (console != null) {
                return console;
            }
            return console = new VelocityConsoleAudience(source, this);
        }

        Player player = (Player) source;
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();
        String locale = Utils.getLocale(player.getPlayerSettings().getLocale());

        return AUDIENCE_CACHE.computeIfAbsent(uuid,
                $ -> new VelocityPlayerAudience(uuid, username, locale, source, true, this));
    }

    @Override
    public @Nullable UserAudience getAudienceByUsername(@NonNull String username) {
        return server.getPlayer(username)
                .map(this::getAudience)
                .orElse(null);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUsername(@NonNull String username) {
        return new VelocityPlayerAudience(null, username, null, null, false, this);
    }

    @Override
    public @Nullable UserAudience getAudienceByUuid(@NonNull UUID uuid) {
        return server.getPlayer(uuid)
                .map(this::getAudience)
                .orElse(null);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUuid(@NonNull UUID uuid) {
        return new VelocityPlayerAudience(uuid, null, null, null, false, this);
    }

    @Override
    public @NonNull Collection<String> getOnlineUsernames(@NonNull PlayerType limitTo) {
        Collection<Player> players = server.getAllPlayers();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (Player player : players) {
                    usernames.add(player.getUsername());
                }
                break;
            case ONLY_JAVA:
                for (Player player : players) {
                    if (!api.isFloodgatePlayer(player.getUniqueId())) {
                        usernames.add(player.getUsername());
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (Player player : players) {
                    if (api.isFloodgatePlayer(player.getUniqueId())) {
                        usernames.add(player.getUsername());
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown PlayerType");
        }
        return usernames;
    }

    @Override
    public boolean hasPermission(Object player, String permission) {
        return cast(player).hasPermission(permission);
    }

    @Override
    public Collection<Object> getOnlinePlayersWithPermission(String permission) {
        List<Object> players = new ArrayList<>();
        for (Player player : server.getAllPlayers()) {
            if (hasPermission(player, permission)) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public void sendMessage(Object target, String locale, TranslatableMessage message,
                            Object... args) {
        ((CommandSource) target).sendMessage(translateAndTransform(locale, message, args));
    }

    @Override
    public void sendMessage(Object target, String message) {
        ((CommandSource) target).sendMessage(Component.text(message));
    }

    @Override
    public void kickPlayer(Object player, String locale, TranslatableMessage message,
                           Object... args) {
        cast(player).disconnect(translateAndTransform(locale, message, args));
    }

    public Component translateAndTransform(
            String locale,
            TranslatableMessage message,
            Object... args) {
        return Component.text(message.translateMessage(manager, locale, args));
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
