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

package org.geysermc.floodgate.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.listener.McListener;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.velocity.player.VelocityConnectionManager;

@Singleton
public final class VelocityListener implements McListener {
    @Inject VelocityConnectionManager connectionManager;
    @Inject ProxyFloodgateConfig config;
    @Inject SimpleFloodgateApi api;
    @Inject LanguageManager languageManager;
    @Inject FloodgateLogger logger;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<String> kickMessageAttribute;

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        Connection player = null;
        String kickMessage;
        try {
            Channel channel = connectionManager.channelFor(event.getConnection());
            player = channel.attr(connectionAttribute).get();
            kickMessage = channel.attr(kickMessageAttribute).get();
        } catch (Exception exception) {
            logger.error("Failed get the FloodgatePlayer from the player's channel", exception);
            kickMessage = "Failed to get the FloodgatePlayer from the player's Channel";
        }

        if (kickMessage != null) {
            event.setResult(
                    PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage))
            );
            return;
        }

        if (player != null) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        Connection connection = connectionManager.connectionByPlatformIdentifier(event.getConnection());
        if (connection == null) {
            return;
        }

        GameProfile profile = new GameProfile(
                connection.javaUuid(),
                connection.javaUsername(),
                Collections.emptyList()
        );
        // The texture properties addition is to fix the February 2 2022 Mojang authentication changes
        if (!config.sendFloodgateData() && !connection.isLinked()) {
            profile = profile.addProperty(new Property("textures", "", ""));
        }
        event.setGameProfile(profile);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(PostLoginEvent event) {
        Connection connection = api.connectionByPlatformIdentifier(event.getPlayer());
        if (connection == null) {
            return;
        }
        languageManager.loadLocale(connection.languageCode());

        // Depending on whether online-mode-kick-existing-players is enabled there are a few events
        // where there are two players online: PermissionSetupEvent and LoginEvent.
        // Only after LoginEvent the duplicated player is kicked. This is the first event after that
        connectionManager.addAcceptedConnection(connection);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        connectionManager.removeConnection(event.getPlayer().getUniqueId());
    }
}
