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

import com.google.common.collect.Queues;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
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
    private final PacketBlocker blocker;
    private final AttributeKey<String> kickMessageAttribute;

    private final Queue<Object> packetQueue = Queues.newConcurrentLinkedQueue();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // prevent other packets from being handled while we handle the handshake packet
        if (!packetQueue.isEmpty()) {
            packetQueue.add(msg);
            return;
        }

        if (msg instanceof PacketWrapper) {
            DefinedPacket packet = ((PacketWrapper) msg).packet;

            // we're only interested in the Handshake packet
            if (packet instanceof Handshake) {
                blocker.enable();
                packetQueue.add(msg);

                handleHandshake(ctx, (Handshake) packet).thenRun(() -> {
                    Object queuedPacket;
                    while ((queuedPacket = packetQueue.poll()) != null) {
                        ctx.fireChannelRead(queuedPacket);
                    }
                    ctx.pipeline().remove(this);
                    blocker.disable();
                });
                return;
            }
        }

        ctx.fireChannelRead(msg);
    }

    private CompletableFuture<Void> handleHandshake(ChannelHandlerContext ctx, Handshake packet) {
        String data = packet.getHost();

        return handler.handle(ctx.channel(), data).thenAccept(result -> {
            HandshakeData handshakeData = result.getHandshakeData();

            // we'll change the IP address from the proxy to the real IP of the client very early on
            // so that almost every plugin will use the real IP of the client
            InetSocketAddress newIp = result.getNewIp(ctx.channel());
            if (newIp != null) {
                HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
                // InitialHandler extends PacketHandler and implements PendingConnection
                InitialHandler connection = ReflectionUtils.getCastedValue(handlerBoss, HANDLER);

                ChannelWrapper channelWrapper =
                        ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);

                channelWrapper.setRemoteAddress(newIp);
            }

            packet.setHost(handshakeData.getHostname());

            if (handshakeData.getDisconnectReason() != null) {
                ctx.channel().attr(kickMessageAttribute).set(handshakeData.getDisconnectReason());
                return;
            }

            switch (result.getResultType()) {
                case EXCEPTION:
                    ctx.channel().attr(kickMessageAttribute)
                            .set(Constants.INTERNAL_ERROR_MESSAGE);
                    break;
                case DECRYPT_ERROR:
                    ctx.channel().attr(kickMessageAttribute)
                            .set(config.getDisconnect().getInvalidKey());
                    break;
                case INVALID_DATA_LENGTH:
                    ctx.channel().attr(kickMessageAttribute)
                            .set(config.getDisconnect().getInvalidArgumentsLength());
                    break;
                default:
                    break;
            }
        }).handle((v, error) -> {
            if (error != null) {
                error.printStackTrace();
            }
            return v;
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.isDebug()) {
            cause.printStackTrace();
        }
    }
}
