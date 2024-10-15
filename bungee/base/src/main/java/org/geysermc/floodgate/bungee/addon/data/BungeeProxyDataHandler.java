/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.bungee.addon.data;

import static java.util.Objects.requireNonNull;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.addon.data.CommonNettyDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.util.ReflectionUtils;

@SuppressWarnings("ConstantConditions")
public class BungeeProxyDataHandler extends CommonNettyDataHandler {
    private static final Field HANDLER;
    private static final Field CHANNEL_WRAPPER;

    static {
        HANDLER = ReflectionUtils.getField(HandlerBoss.class, "handler");
        requireNonNull(HANDLER, "handler field cannot be null");

        CHANNEL_WRAPPER = ReflectionUtils.getFieldOfType(InitialHandler.class, ChannelWrapper.class);
        requireNonNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");
    }

    public BungeeProxyDataHandler(
            DataSeeker dataSeeker,
            FloodgateDataHandler handshakeHandler,
            ProxyFloodgateConfig config,
            FloodgateLogger logger,
            AttributeKey<Connection> connectionAttribute,
            AttributeKey<Component> kickMessageAttribute,
            PacketBlocker blocker) {
        super(dataSeeker, handshakeHandler, config, logger, connectionAttribute, kickMessageAttribute, blocker);
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
        // InitialHandler extends PacketHandler and implements PendingConnection
        InitialHandler connection = ReflectionUtils.getCastedValue(handlerBoss, HANDLER);

        ChannelWrapper channelWrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);
        channelWrapper.setRemoteAddress(newIp);
    }

    @Override
    protected Object setHostname(Object wrapperWithHandshake, String hostname) {
        PacketWrapper wrapper = (PacketWrapper) wrapperWithHandshake;
        Handshake handshake = (Handshake) wrapper.packet;
        handshake.setHost(hostname);
        return wrapper;
    }

    @Override
    public boolean channelRead(Object msg) {
        if (msg instanceof PacketWrapper) {
            DefinedPacket packet = ((PacketWrapper) msg).packet;

            // we're only interested in the Handshake packet
            if (packet instanceof Handshake) {
                handle(msg, ((Handshake) packet).getHost());

                // otherwise, it'll get read twice. once by the packet queue and once by this method
                return false;
            }
        }

        return true;
    }
}
