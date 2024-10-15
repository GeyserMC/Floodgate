/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.spigot.addon.data;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.api.inject.InjectorAddon;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.logger.FloodgateLogger;

@Singleton
public final class SpigotDataAddon implements InjectorAddon<Channel> {
    @Inject
    DataSeeker dataSeeker;

    @Inject
    FloodgateDataHandler handshakeHandler;

    @Inject
    FloodgateConfig config;

    @Inject
    FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    String packetHandlerName;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<Component> kickMessageAttribute;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        var dataHandler = new SpigotDataHandler(
                dataSeeker, handshakeHandler, config, logger, connectionAttribute, kickMessageAttribute);

        // we have to add the packet blocker in the data handler, otherwise ProtocolSupport breaks
        channel.pipeline().addBefore(packetHandlerName, "floodgate_data_handler", dataHandler);
    }

    @Override
    public void onRemoveInject(Channel channel) {}

    @Override
    public boolean shouldInject() {
        return true;
    }
}
