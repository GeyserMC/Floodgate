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

package com.minekube.connect.addon.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minekube.connect.util.ReflectionUtils.getField;
import static com.minekube.connect.util.ReflectionUtils.getPrefixedClass;

import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.config.FloodgateConfig;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;

public final class VelocityProxyDataHandler extends CommonDataHandler {
    private static final Field HANDSHAKE;
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_SERVER_ADDRESS;
    private static final Field REMOTE_ADDRESS;

    static {
        Class<?> iic = getPrefixedClass("connection.client.InitialInboundConnection");
        checkNotNull(iic, "InitialInboundConnection class cannot be null");

        HANDSHAKE = getField(iic, "handshake");
        checkNotNull(HANDSHAKE, "Handshake field cannot be null");

        HANDSHAKE_PACKET = getPrefixedClass("protocol.packet.Handshake");
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_SERVER_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_SERVER_ADDRESS, "Address in the Handshake packet cannot be null");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");
        REMOTE_ADDRESS = getField(minecraftConnection, "remoteAddress");
    }

    private final FloodgateLogger logger;

    public VelocityProxyDataHandler(
            FloodgateConfig config,
            PacketBlocker blocker,
            AttributeKey<String> kickMessageAttribute,
            FloodgateLogger logger) {
        super(config, kickMessageAttribute, blocker);
        this.logger = logger;
    }
//
//    @Override
//    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
//        setValue(channel.pipeline().get("handler"), REMOTE_ADDRESS, newIp);
//    }
//
//    @Override
//    protected Object setHostname(Object handshakePacket, String hostname) {
//        setValue(handshakePacket, HANDSHAKE_SERVER_ADDRESS, hostname);
//        return handshakePacket;
//    }
//
//    @Override
//    protected boolean shouldRemoveHandler(HandshakeResult result) {
//        if (result.getResultType() == ResultType.SUCCESS) {
//            FloodgatePlayer player = result.getFloodgatePlayer();
//            logger.info("Floodgate player who is logged in as {} {} joined",
//                    player.getUsername(), player.getUniqueId());
//        }
//        return super.shouldRemoveHandler(result);
//    }
//
//    @Override
//    public boolean channelRead(Object packet) {
//        // we're only interested in the Handshake packet.
//        // it should be the first packet but you never know
//        if (HANDSHAKE_PACKET.isInstance(packet)) {
//            handle(packet, getCastedValue(packet, HANDSHAKE_SERVER_ADDRESS));
//            // otherwise, it'll get read twice. once by the packet queue and once by this method
//            return false;
//        }
//        return true;
//    }
}
