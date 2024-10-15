/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.velocity.addon.data;

import static java.util.Objects.requireNonNull;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.core.util.ReflectionUtils.setValue;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.addon.data.CommonNettyDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler.HandleResult;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler.HandleResultType;
import org.geysermc.floodgate.core.logger.FloodgateLogger;

public final class VelocityProxyDataHandler extends CommonNettyDataHandler {
    private static final Field REMOTE_ADDRESS;
    private static final Field FORCE_KEY_AUTHENTICATION;

    static {
        REMOTE_ADDRESS = getField(MinecraftConnection.class, "remoteAddress");
        requireNonNull(REMOTE_ADDRESS, "remoteAddress cannot be null");

        FORCE_KEY_AUTHENTICATION = getField(InitialLoginSessionHandler.class, "forceKeyAuthentication");
        requireNonNull(FORCE_KEY_AUTHENTICATION, "forceKeyAuthentication cannot be null");
    }

    private final FloodgateLogger logger;

    public VelocityProxyDataHandler(
            DataSeeker dataSeeker,
            FloodgateDataHandler handshakeHandler,
            FloodgateConfig config,
            PacketBlocker blocker,
            AttributeKey<Connection> connectionAttribute,
            AttributeKey<Component> kickMessageAttribute,
            FloodgateLogger logger) {
        super(dataSeeker, handshakeHandler, config, logger, connectionAttribute, kickMessageAttribute, blocker);
        this.logger = logger;
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        setValue(channel.pipeline().get("handler"), REMOTE_ADDRESS, newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) {
        ((HandshakePacket) handshakePacket).setServerAddress(hostname);
        return handshakePacket;
    }

    @Override
    protected boolean shouldRemoveHandler(HandleResult result) {
        if (result.type() == HandleResultType.SUCCESS) {
            // the way Velocity stores whether to force key authentication
            // we need the login packet to bypass the 'force key authentication'
            return !Boolean.getBoolean("auth.forceSecureProfiles");
        }
        return super.shouldRemoveHandler(result);
    }

    @Override
    public boolean channelRead(Object packet) {
        if (packet instanceof HandshakePacket handshake) {
            handle(packet, handshake.getServerAddress());
            // otherwise, it'll get read twice. once by the packet queue and once by this method
            return false;
        }

        // at this point we know that forceKeyAuthentication is enabled
        if (packet instanceof ServerLoginPacket) {
            MinecraftConnection minecraftConnection =
                    (MinecraftConnection) ctx.pipeline().get("handler");
            MinecraftSessionHandler sessionHandler = minecraftConnection.getActiveSessionHandler();
            if (!(sessionHandler instanceof InitialLoginSessionHandler)) {
                logger.error("Expected player's session handler to be InitialLoginSessionHandler");
                return true;
            }
            setValue(sessionHandler, FORCE_KEY_AUTHENTICATION, false);
        }
        return true;
    }
}
