/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.velocity.addon.data;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.api.inject.InjectorAddon;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.logger.FloodgateLogger;

@Singleton
public final class VelocityDataAddon implements InjectorAddon<Channel> {
    @Inject
    DataSeeker dataSeeker;

    @Inject
    FloodgateDataHandler handshakeHandler;

    @Inject
    ProxyFloodgateConfig config;

    @Inject
    FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    String packetHandler;

    @Inject
    @Named("packetDecoder")
    String packetDecoder;

    @Inject
    @Named("packetEncoder")
    String packetEncoder;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<Component> kickMessageAttribute;

    @Inject
    VelocityServerDataHandler serverDataHandler;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        if (toServer) {
            if (config.sendFloodgateData()) {
                channel.pipeline().addAfter(packetEncoder, "floodgate_data_handler", serverDataHandler);
            }
            return;
        }

        PacketBlocker blocker = new PacketBlocker();
        channel.pipeline().addBefore(packetDecoder, "floodgate_packet_blocker", blocker);

        var dataHandler = new VelocityProxyDataHandler(
                dataSeeker, handshakeHandler, config, blocker, connectionAttribute, kickMessageAttribute, logger);

        // The handler is already added so we should add our handler before it
        channel.pipeline().addBefore(packetHandler, "floodgate_data_handler", dataHandler);
    }

    @Override
    public void onRemoveInject(Channel channel) {}

    @Override
    public boolean shouldInject() {
        return true;
    }
}
