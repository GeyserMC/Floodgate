/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.bungee.listener;

import static java.util.Objects.requireNonNull;

import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.lang.reflect.Field;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.bungee.player.BungeeConnectionManager;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.listener.McListener;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.core.skin.SkinDataImpl;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.core.util.ReflectionUtils;

@Singleton
public final class BungeeListener implements Listener, McListener {
    private static final Field PLAYER_NAME;

    static {
        PLAYER_NAME = ReflectionUtils.getField(InitialHandler.class, "name");
        requireNonNull(PLAYER_NAME, "Initial name field cannot be null");
    }

    @Inject BungeeConnectionManager connectionManager;
    @Inject ProxyFloodgateConfig config;
    @Inject SimpleFloodgateApi api;
    @Inject LanguageManager languageManager;
    @Inject SkinApplier skinApplier;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<String> kickMessageAttribute;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(PreLoginEvent event) {
        // well, no reason to check if the player will be kicked anyway
        if (event.isCancelled()) {
            return;
        }

        PendingConnection pendingConnection = event.getConnection();
        var channel = connectionManager.channelFor(pendingConnection);

        // check if the player has to be kicked
        String kickReason = channel.attr(kickMessageAttribute).get();
        if (kickReason != null) {
            event.setCancelled(true);
            event.setCancelReason(kickReason);
            return;
        }

        Connection connection = connectionManager.connectionByPlatformIdentifier(channel);
        if (connection == null) {
            return;
        }
        pendingConnection.setOnlineMode(false);
        pendingConnection.setUniqueId(connection.javaUuid());
        ReflectionUtils.setValue(pendingConnection, PLAYER_NAME, connection.javaUsername());
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        // if there was another player with the same uuid / name online,
        // he has been disconnected by now
        Connection connection = api.connectionByPlatformIdentifier(event.getConnection());
        if (connection == null) {
            return;
        }

        languageManager.loadLocale(connection.languageCode());
        connectionManager.addAcceptedConnection(connection);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostLogin(PostLoginEvent event) {
        // To fix the February 2 2022 Mojang authentication changes
        if (!config.sendFloodgateData()) {
            Connection connection = api.connectionByPlatformIdentifier(event.getPlayer());
            if (connection != null && !connection.isLinked()) {
                skinApplier.applySkin(connection, new SkinDataImpl("", ""));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        connectionManager.removeConnection(event.getPlayer());
    }
}
