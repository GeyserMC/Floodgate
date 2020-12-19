/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import static org.geysermc.floodgate.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.util.ReflectionUtils.getPrefixedClass;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.HandshakeHandler;
import org.geysermc.floodgate.HandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;

@RequiredArgsConstructor
public final class VelocityProxyDataHandler extends SimpleChannelInboundHandler<Object> {
    private static final Field HANDSHAKE;
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_SERVER_ADDRESS;

    static {
        Class<?> iic = getPrefixedClass("connection.client.InitialInboundConnection");
        checkNotNull(iic, "InitialInboundConnection class cannot be null");

        HANDSHAKE = getField(iic, "handshake");
        checkNotNull(HANDSHAKE, "Handshake field cannot be null");

        HANDSHAKE_PACKET = getPrefixedClass("protocol.packet.Handshake");
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_SERVER_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_SERVER_ADDRESS, "Address in the Handshake packet cannot be null");
    }

    private final ProxyFloodgateConfig config;
    private final ProxyFloodgateApi api;
    private final HandshakeHandler handshakeHandler;
    private final AttributeKey<FloodgatePlayer> playerAttribute;
    private final AttributeKey<String> kickMessageAttribute;
    private final FloodgateLogger logger;
    private boolean done;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        ReferenceCountUtil.retain(msg);
        // we're only interested in the Handshake packet
        if (done || !HANDSHAKE_PACKET.isInstance(msg)) {
            ctx.fireChannelRead(msg);
            done = true;
            return;
        }

        handleClientToProxy(ctx, msg);
        ctx.fireChannelRead(msg);
        done = true;
    }

    private void handleClientToProxy(ChannelHandlerContext ctx, Object packet) {
        String address = getCastedValue(packet, HANDSHAKE_SERVER_ADDRESS);

        HandshakeResult result = handshakeHandler.handle(ctx.channel(), address);
        switch (result.getResultType()) {
            case SUCCESS:
                break;
            case EXCEPTION:
                ctx.channel().attr(kickMessageAttribute).set(config.getDisconnect().getInvalidKey());
            case INVALID_DATA_LENGTH:
                ctx.channel().attr(kickMessageAttribute)
                        .set(config.getDisconnect().getInvalidArgumentsLength());
                return;
            default:
                return;
        }

        FloodgatePlayer player = result.getFloodgatePlayer();

        // we can't rely on Velocity when it comes to kicking the old players, so with this
        // system we only have to check if the connection (which is already closed at that time)
        // has the FloodgatePlayer attribute
        ctx.channel().attr(playerAttribute).set(player);

        api.addEncryptedData(player.getCorrectUniqueId(), result.getHandshakeData()[1]);
        logger.info("Floodgate player who is logged in as {} {} joined",
                player.getCorrectUsername(), player.getCorrectUniqueId());
    }
}
