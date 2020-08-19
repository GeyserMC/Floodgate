/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.kyori.adventure.text.TextComponent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LanguageManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.geysermc.floodgate.util.ReflectionUtil.*;

public final class VelocityListener {
    private static final Field INITIAL_MINECRAFT_CONNECTION;
    private static final Field MINECRAFT_CONNECTION;
    private static final Field CHANNEL;

    private final ProxyFloodgateApi api;
    private final AttributeKey<FloodgatePlayer> playerAttribute;
    private final AttributeKey<String> kickMessageAttribute;
    private final FloodgateLogger logger;
    private final LanguageManager languageManager;

    private final Cache<InboundConnection, FloodgatePlayer> playerCache;

    public VelocityListener(ProxyFloodgateApi api,
                            AttributeKey<FloodgatePlayer> playerAttribute,
                            AttributeKey<String> kickMessageAttribute,
                            FloodgateLogger logger, LanguageManager languageManager) {
        this.api = api;
        this.playerAttribute = playerAttribute;
        this.kickMessageAttribute = kickMessageAttribute;
        this.logger = logger;
        this.languageManager = languageManager;

        this.playerCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(20, TimeUnit.SECONDS)
                .build();
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPreLogin(PreLoginEvent event) {
        FloodgatePlayer player = null;
        String kickMessage;
        try {
            Object mcConnection = getValue(event.getConnection(), INITIAL_MINECRAFT_CONNECTION);
            Channel channel = getCastedValue(mcConnection, CHANNEL);

            player = channel.attr(playerAttribute).get();
            if (player != null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            }

            kickMessage = channel.attr(kickMessageAttribute).get();
        } catch (Exception exception) {
            logger.error("Failed get the FloodgatePlayer from the player's channel", exception);
            kickMessage = "Failed to get the FloodgatePlayer from the players's Channel";
        }

        if (kickMessage != null) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(TextComponent.of(kickMessage)));
            return;
        }

        if (player != null) {
            playerCache.put(event.getConnection(), player);
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        FloodgatePlayer player = playerCache.getIfPresent(event.getConnection());
        if (player != null) {
            playerCache.invalidate(event.getConnection());
            event.setGameProfile(new GameProfile(
                    player.getCorrectUniqueId(), player.getCorrectUsername(), new ArrayList<>()));
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        FloodgatePlayer player =
                FloodgateApi.getInstance().getPlayer(event.getPlayer().getUniqueId());
        if (player != null) {
            languageManager.loadFloodgateLocale(player.getLanguageCode());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        try {
            Object minecraftConnection = getValue(player, MINECRAFT_CONNECTION);
            Channel channel = getCastedValue(minecraftConnection, CHANNEL);
            FloodgatePlayer fPlayer = channel.attr(playerAttribute).get();

            if (fPlayer != null && api.removePlayer(fPlayer)) {
                api.removeEncryptedData(event.getPlayer().getUniqueId());
                logger.info(languageManager.getLocaleStringLog(
                        "floodgate.ingame.disconnect_name", player.getUsername())
                );
            }
        } catch (Exception exception) {
            logger.error("Failed to remove the player", exception);
        }
    }

    static {
        Class<?> initialConnection = getPrefixedClass("connection.client.InitialInboundConnection");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");
        INITIAL_MINECRAFT_CONNECTION = getFieldOfType(initialConnection, minecraftConnection, true);
        Class<?> connectedPlayer = getPrefixedClass("connection.client.ConnectedPlayer");
        MINECRAFT_CONNECTION = getFieldOfType(connectedPlayer, minecraftConnection, true);
        CHANNEL = getFieldOfType(minecraftConnection, Channel.class, true);
    }
}
