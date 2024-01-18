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

package org.geysermc.floodgate.addon.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.util.ReflectionUtils.getClassOrFallbackPrefixed;
import static org.geysermc.floodgate.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethodByName;
import static org.geysermc.floodgate.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.util.ReflectionUtils.invoke;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.ResultType;

public final class VelocityProxyDataHandler extends CommonDataHandler {
    private static final Field HANDSHAKE;
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_SERVER_ADDRESS;
    private static final Field REMOTE_ADDRESS;

    private static final Class<?> SERVER_LOGIN_PACKET;
    private static final Method GET_SESSION_HANDLER;
    private static final Class<?> INITIAL_LOGIN_SESSION_HANDLER;
    private static final Field FORCE_KEY_AUTHENTICATION;

    static {
        Class<?> iic = getPrefixedClass("connection.client.InitialInboundConnection");
        checkNotNull(iic, "InitialInboundConnection class cannot be null");

        HANDSHAKE = getField(iic, "handshake");
        checkNotNull(HANDSHAKE, "Handshake field cannot be null");

        HANDSHAKE_PACKET = getClassOrFallbackPrefixed(
                "protocol.packet.HandshakePacket",
                "protocol.packet.Handshake"
        );
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_SERVER_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_SERVER_ADDRESS, "Address in the Handshake packet cannot be null");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");
        REMOTE_ADDRESS = getField(minecraftConnection, "remoteAddress");
        checkNotNull(REMOTE_ADDRESS, "remoteAddress cannot be null");

        SERVER_LOGIN_PACKET = getClassOrFallbackPrefixed(
                "protocol.packet.ServerLoginPacket",
                "protocol.packet.ServerLogin"
        );
        checkNotNull(SERVER_LOGIN_PACKET, "ServerLogin packet class cannot be null");

        
        Method sessionHandler = getMethodByName(minecraftConnection, "getSessionHandler", true);
        if (sessionHandler == null) {
            // We are 1.20.2+
            sessionHandler = getMethodByName(minecraftConnection, "getActiveSessionHandler", true);
        }
        GET_SESSION_HANDLER = sessionHandler;
        checkNotNull(GET_SESSION_HANDLER, "getSessionHandler method cannot be null");

        INITIAL_LOGIN_SESSION_HANDLER =
                getPrefixedClass("connection.client.InitialLoginSessionHandler");
        checkNotNull(INITIAL_LOGIN_SESSION_HANDLER, "InitialLoginSessionHandler cannot be null");

        // allowed to be null if it's an old Velocity version
        FORCE_KEY_AUTHENTICATION = getField(INITIAL_LOGIN_SESSION_HANDLER, "forceKeyAuthentication");
    }

    private final FloodgateLogger logger;

    public VelocityProxyDataHandler(
            FloodgateConfig config,
            FloodgateHandshakeHandler handshakeHandler,
            PacketBlocker blocker,
            AttributeKey<String> kickMessageAttribute,
            FloodgateLogger logger) {
        super(handshakeHandler, config, kickMessageAttribute, blocker);
        this.logger = logger;
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        setValue(channel.pipeline().get("handler"), REMOTE_ADDRESS, newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) {
        setValue(handshakePacket, HANDSHAKE_SERVER_ADDRESS, hostname);
        return handshakePacket;
    }

    @Override
    protected boolean shouldRemoveHandler(HandshakeResult result) {
        if (result.getResultType() == ResultType.SUCCESS) {
            FloodgatePlayer player = result.getFloodgatePlayer();
            logger.info("Floodgate player who is logged in as {} {} joined",
                    player.getCorrectUsername(), player.getCorrectUniqueId());

            // the way Velocity stores whether to force key authentication
            boolean forceKeyAuthentication = Boolean.getBoolean("auth.forceSecureProfiles");
            // we need the login packet to bypass the 'force key authentication'
            return !forceKeyAuthentication;
        }
        return super.shouldRemoveHandler(result);
    }

    @Override
    public boolean channelRead(Object packet) {
        if (HANDSHAKE_PACKET.isInstance(packet)) {
            handle(packet, getCastedValue(packet, HANDSHAKE_SERVER_ADDRESS));
            // otherwise, it'll get read twice. once by the packet queue and once by this method
            return false;
        }

        // at this point we know that forceKeyAuthentication is enabled
        if (SERVER_LOGIN_PACKET.isInstance(packet)) {
            Object minecraftConnection = ctx.pipeline().get("handler");
            Object sessionHandler = invoke(minecraftConnection, GET_SESSION_HANDLER);
            if (!INITIAL_LOGIN_SESSION_HANDLER.isInstance(sessionHandler)) {
                logger.error("Expected player's session handler to be InitialLoginSessionHandler");
                return true;
            }
            if (FORCE_KEY_AUTHENTICATION != null) {
                setValue(sessionHandler, FORCE_KEY_AUTHENTICATION, false);
            }
        }
        return true;
    }
}
