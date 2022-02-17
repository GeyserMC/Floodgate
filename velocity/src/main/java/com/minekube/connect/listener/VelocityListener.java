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

package com.minekube.connect.listener;

import static com.minekube.connect.util.ReflectionUtils.getCastedValue;
import static com.minekube.connect.util.ReflectionUtils.getField;
import static com.minekube.connect.util.ReflectionUtils.getFieldOfType;
import static com.minekube.connect.util.ReflectionUtils.getPrefixedClass;
import static com.minekube.connect.util.ReflectionUtils.getPrefixedClassSilently;
import static com.minekube.connect.util.ReflectionUtils.getValue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.minekube.connect.api.ProxyFloodgateApi;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.api.player.FloodgatePlayer;
import com.minekube.connect.network.netty.ChannelWrapper;
import com.minekube.connect.util.LanguageManager;
import com.minekube.connect.util.VelocityCommandUtil;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class VelocityListener {
    private static final Field INITIAL_MINECRAFT_CONNECTION;
    private static final Field INITIAL_CONNECTION_DELEGATE;
    private static final Field CHANNEL;

    static {
        Class<?> initialConnection = getPrefixedClass("connection.client.InitialInboundConnection");
        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");
        INITIAL_MINECRAFT_CONNECTION = getFieldOfType(initialConnection, minecraftConnection);

        // Since Velocity 3.1.0
        Class<?> loginInboundConnection =
                getPrefixedClassSilently("connection.client.LoginInboundConnection");
        if (loginInboundConnection != null) {
            INITIAL_CONNECTION_DELEGATE = getField(loginInboundConnection, "delegate");
            Objects.requireNonNull(
                    INITIAL_CONNECTION_DELEGATE,
                    "initial inbound connection delegate cannot be null"
            );
        } else {
            INITIAL_CONNECTION_DELEGATE = null;
        }

        CHANNEL = getFieldOfType(minecraftConnection, Channel.class);
    }

    private final Cache<InboundConnection, FloodgatePlayer> playerCache =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .expireAfterAccess(20, TimeUnit.SECONDS)
                    .build();

    @Inject private ProxyFloodgateApi api;
    @Inject private LanguageManager languageManager;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("playerAttribute")
    private AttributeKey<FloodgatePlayer> playerAttribute;

    @Subscribe(order = PostOrder.EARLY)
    public void onHS(ConnectionHandshakeEvent event) {
        System.out.println(event.toString());
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPreLogin(PreLoginEvent event) {
        System.out.println(event.toString());
        System.out.println("Remote " + event.getConnection().getRemoteAddress());

        FloodgatePlayer player = null;
        try {
            InboundConnection connection = event.getConnection();
            System.out.println("a " + connection + " " + connection.getClass());
            if (INITIAL_CONNECTION_DELEGATE != null) {
                // Velocity 3.1.0 added LoginInboundConnection which is used in the login state,
                // but that class doesn't have a Channel field. However, it does have
                // InitialInboundConnection as a field
                connection = getCastedValue(connection, INITIAL_CONNECTION_DELEGATE);
            }
            Object mcConnection = getValue(connection, INITIAL_MINECRAFT_CONNECTION);
            Channel channel = getCastedValue(mcConnection, CHANNEL);

            System.out.println("b " + connection + " " + connection.getClass());
            System.out.println("c " + mcConnection + " " + mcConnection.getClass());
            System.out.println("d " + channel + " " + channel.getClass());
            ChannelWrapper cw = (ChannelWrapper) channel;
            System.out.println(cw.getWMyData());
            player = channel.attr(playerAttribute).get();
        } catch (Exception exception) {
            logger.error("Failed get the FloodgatePlayer from the player's channel", exception);
        }

        System.out.println(player);
        if (player != null) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            playerCache.put(event.getConnection(), player);
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        if (event.isOnlineMode()) {
            return;
        }
//        event.getConnection() TODO player from channel wrapper?
        FloodgatePlayer player = playerCache.getIfPresent(event.getConnection());
        if (player != null) {
            playerCache.invalidate(event.getConnection());
            // Use the game profile received from WatchService for this connection
            event.setGameProfile(gameProfileFromPlayer(event.getGameProfile(), player));
        }
    }

    private GameProfile gameProfileFromPlayer(GameProfile default0, FloodgatePlayer player) {
        return default0
                .withId(player.getUniqueId())
                .withName(player.getUsername())
                .addProperties(player.getProperties().stream()
                        .map(p -> new Property(p.getName(), p.getValue(), p.getSignature()))
                        .collect(Collectors.toList()));
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLogin(LoginEvent event) {
        if (event.getResult().isAllowed()) {
            FloodgatePlayer player = api.getPlayer(event.getPlayer().getUniqueId());
            if (player != null) {
                languageManager.loadLocale(player.getLanguageCode());
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        api.playerRemoved(event.getPlayer().getUniqueId());

        VelocityCommandUtil.AUDIENCE_CACHE.remove(event.getPlayer().getUniqueId()); //todo
    }
}
