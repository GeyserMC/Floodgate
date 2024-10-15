/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.velocity.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.listener.McListener;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.core.util.MojangUtils;
import org.geysermc.floodgate.velocity.player.VelocityConnectionManager;

@Singleton
public final class VelocityListener implements McListener {
    private static final Property DEFAULT_TEXTURE_PROPERTY = new Property(
            "textures", Constants.DEFAULT_MINECRAFT_JAVA_SKIN_TEXTURE, Constants.DEFAULT_MINECRAFT_JAVA_SKIN_SIGNATURE);

    @Inject
    VelocityConnectionManager connectionManager;

    @Inject
    SimpleFloodgateApi api;

    @Inject
    LanguageManager languageManager;

    @Inject
    FloodgateLogger logger;

    @Inject
    MojangUtils mojangUtils;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<Component> kickMessageAttribute;

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        Connection player = null;
        Component kickMessage;
        try {
            Channel channel = connectionManager.channelFor(event.getConnection());
            player = channel.attr(connectionAttribute).get();
            kickMessage = channel.attr(kickMessageAttribute).get();
        } catch (Exception exception) {
            logger.error("Failed get the FloodgatePlayer from the player's channel", exception);
            kickMessage = Component.text("Failed to get the FloodgatePlayer from the player's Channel");
        }

        if (kickMessage != null) {
            event.setResult(PreLoginComponentResult.denied(kickMessage));
            return;
        }

        if (player != null) {
            event.setResult(PreLoginComponentResult.forceOfflineMode());
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onGameProfileRequest(GameProfileRequestEvent event, Continuation continuation) {
        Connection connection = connectionManager.connectionByPlatformIdentifier(event.getConnection());
        if (connection == null) {
            continuation.resume();
            return;
        }

        // Skin look up (on Spigot and friends) would result in it failing, so apply a default skin
        if (!connection.isLinked()) {
            event.setGameProfile(new GameProfile(
                    connection.javaUuid(), connection.javaUsername(), List.of(DEFAULT_TEXTURE_PROPERTY)));
            continuation.resume();
            return;
        }

        // Floodgate players are seen as offline mode players, meaning we have to look up
        // the linked player's textures ourselves

        mojangUtils.skinFor(connection.javaUuid()).thenAccept(skin -> {
            event.setGameProfile(new GameProfile(
                    connection.javaUuid(),
                    connection.javaUsername(),
                    List.of(new Property("textures", skin.value(), skin.signature()))));
            continuation.resume();
        });
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
