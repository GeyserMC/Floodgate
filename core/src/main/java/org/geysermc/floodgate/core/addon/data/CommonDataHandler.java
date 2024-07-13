/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.addon.data;

import com.google.common.collect.Queues;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.core.player.HostnameSeparationResult;
import org.geysermc.floodgate.util.Constants;

@RequiredArgsConstructor
public abstract class CommonDataHandler extends ChannelInboundHandlerAdapter {
    protected final FloodgateHandshakeHandler handshakeHandler;
    protected final FloodgateConfig config;
    protected final AttributeKey<String> kickMessageAttribute;
    protected final PacketBlocker blocker;

    protected final Queue<Object> packetQueue = Queues.newConcurrentLinkedQueue();
    protected Object handshakePacket;
    protected ChannelHandlerContext ctx;

    protected abstract void setNewIp(Channel channel, InetSocketAddress newIp);

    protected abstract Object setHostname(Object handshakePacket, String hostname);

    protected abstract boolean channelRead(Object packet) throws Exception;

    protected boolean shouldRemoveHandler(HandshakeResult result) {
        return true;
    }

    protected boolean shouldCallFireRead(Object queuedPacket) {
        return true;
    }

    protected void handle(Object handshakePacket, String hostname) {
        this.handshakePacket = handshakePacket;
        HostnameSeparationResult separation = handshakeHandler.separateHostname(hostname);

        if (separation.floodgateData() == null) {
            // not a Floodgate player, make sure to resend the cancelled handshake packet
            disablePacketQueue(true);
            return;
        }

        if (separation.headerVersion() != FloodgateCipher.VERSION) {
            disablePacketQueue(true);
            setKickMessage(String.format(
                    Constants.UNSUPPORTED_DATA_VERSION,
                    FloodgateCipher.VERSION, separation.headerVersion()
            ));
            return;
        }

        blocker.enable();

        Channel channel = ctx.channel();

        handshakeHandler
                .handle(channel, separation.floodgateData(), separation.hostnameRemainder())
                .thenApply(result -> {
                    HandshakeData handshakeData = result.getHandshakeData();

                    // we'll change the IP address to the real IP of the client very early on
                    // so that almost every plugin will use the real IP of the client
                    InetSocketAddress newIp = result.getNewIp(channel);
                    if (newIp != null) {
                        setNewIp(channel, newIp);
                    }

                    this.handshakePacket = setHostname(handshakePacket, handshakeData.getHostname());

                    if (handshakeData.shouldDisconnect()) {
                        setKickMessage(handshakeData.getDisconnectReason());
                        return shouldRemoveHandler(result);
                    }

                    switch (result.getResultType()) {
                        case EXCEPTION:
                            setKickMessage(Constants.INTERNAL_ERROR_MESSAGE);
                            break;
                        case DECRYPT_ERROR:
                            setKickMessage(config.getDisconnect().getInvalidKey());
                            break;
                        case INVALID_DATA_LENGTH:
                            setKickMessage(config.getDisconnect().getInvalidArgumentsLength());
                            break;
                        default:
                            break;
                    }
                    return shouldRemoveHandler(result);
                }).handle((shouldRemove, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                    }
                    disablePacketQueue(shouldRemove);
                    return shouldRemove;
                });
    }

    protected void disablePacketQueue(boolean removeSelf) {
        if (handshakePacket != null && shouldCallFireRead(handshakePacket)) {
            ctx.fireChannelRead(handshakePacket);
        }

        Object queuedPacket;
        while ((queuedPacket = packetQueue.poll()) != null) {
            if (shouldCallFireRead(queuedPacket)) {
                ctx.fireChannelRead(queuedPacket);
            }
        }
        if (removeSelf) {
            removeSelf();
        }
        blocker.disable();
    }

    protected void removeSelf() {
        ctx.pipeline().remove(this);
    }

    protected final void setKickMessage(String message) {
        ctx.channel().attr(kickMessageAttribute).set(message);
    }

    protected final String getKickMessage() {
        return ctx.channel().attr(kickMessageAttribute).get();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) {
        // prevent other packets from being handled while we handle the handshake packet
        if (!packetQueue.isEmpty()) {
            packetQueue.add(packet);
            return;
        }

        try {
            if (channelRead(packet)) {
                ctx.fireChannelRead(packet);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.isDebug()) {
            cause.printStackTrace();
        }
    }
}
