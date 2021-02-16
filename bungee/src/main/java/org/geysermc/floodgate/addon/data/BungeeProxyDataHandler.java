/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.ReflectionUtils;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor
public class BungeeProxyDataHandler extends ChannelInboundHandlerAdapter {
    private static final Field HANDLER;
    private static final Field CHANNEL_WRAPPER;

    static {
        HANDLER = ReflectionUtils.getField(HandlerBoss.class, "handler");
        checkNotNull(HANDLER, "handler field cannot be null");

        CHANNEL_WRAPPER =
                ReflectionUtils.getFieldOfType(InitialHandler.class, ChannelWrapper.class);
        checkNotNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");
    }

    private final ProxyFloodgateConfig config;
    private final FloodgateHandshakeHandler handler;
    private final AttributeKey<String> kickMessageAttribute;
    private boolean done;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ReferenceCountUtil.retain(msg);
        if (done || !(msg instanceof PacketWrapper)) {
            ctx.fireChannelRead(msg);
            return;
        }

        DefinedPacket packet = ((PacketWrapper) msg).packet;

        // we're only interested in the Handshake packet
        if (packet instanceof Handshake) {
            handleHandshake(ctx, (Handshake) packet);
            done = true;
        }

        ctx.fireChannelRead(msg);
    }

    private void handleHandshake(ChannelHandlerContext ctx, Handshake packet) {
        String data = packet.getHost();

        HandshakeResult result = handler.handle(ctx.channel(), data);
        HandshakeData handshakeData = result.getHandshakeData();

        // we'll change the IP address from the proxy to the IP of the Bedrock client very early on
        // so that almost every plugin will use the IP of the Bedrock client
        if (result.getFloodgatePlayer() != null) {

            HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
            // InitialHandler extends PacketHandler and implements PendingConnection
            InitialHandler connection = ReflectionUtils.getCastedValue(handlerBoss, HANDLER);

            ChannelWrapper channelWrapper =
                    ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);

            InetSocketAddress address =
                    result.getFloodgatePlayer().getProperty(PropertyKey.SOCKET_ADDRESS);

            channelWrapper.setRemoteAddress(address);
        }

        if (handshakeData.getDisconnectReason() != null) {
            ctx.channel().attr(kickMessageAttribute).set(handshakeData.getDisconnectReason());
            return;
        }

        switch (result.getResultType()) {
            case EXCEPTION:
                ctx.channel().attr(kickMessageAttribute).set(
                        config.getDisconnect().getInvalidKey());
                break;
            case INVALID_DATA_LENGTH:
                ctx.channel().attr(kickMessageAttribute)
                        .set(config.getDisconnect().getInvalidArgumentsLength());
                break;
            case TIMESTAMP_DENIED:
                ctx.channel().attr(kickMessageAttribute).set(Constants.TIMESTAMP_DENIED_MESSAGE);
                break;
            default:
                break;
        }
    }
}
