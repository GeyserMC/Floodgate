/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.addon.data;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.DataSeeker.DataSeekerResult;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler.HandleResult;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler.HandleResultType;
import org.geysermc.floodgate.core.crypto.exception.UnsupportedVersionException;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.util.InvalidFormatException;

@RequiredArgsConstructor
public abstract class CommonNettyDataHandler extends ChannelInboundHandlerAdapter {
    protected final DataSeeker dataSeeker;
    protected final FloodgateDataHandler handshakeHandler;
    protected final FloodgateConfig config;
    protected final FloodgateLogger logger;
    protected final AttributeKey<Connection> connectionAttribute;
    protected final AttributeKey<Component> kickMessageAttribute;
    protected final PacketBlocker blocker;

    protected final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();
    protected Object handshakePacket;
    protected ChannelHandlerContext ctx;

    protected abstract void setNewIp(Channel channel, InetSocketAddress newIp);

    protected abstract Object setHostname(Object handshakePacket, String hostname);

    protected abstract boolean channelRead(Object packet) throws Exception;

    // todo rewrite this method
    protected boolean shouldRemoveHandler(HandleResult result) {
        return true;
    }

    protected boolean shouldCallFireRead(Object queuedPacket) {
        return true;
    }

    protected void handle(Object handshakePacket, String hostname) {
        this.handshakePacket = handshakePacket;

        Channel channel = ctx.channel();

        DataSeekerResult seekResult;
        try {
            seekResult = dataSeeker.seekData(hostname, channel);
        } catch (InvalidFormatException ignored) {
            disablePacketQueue(new HandleResult(HandleResultType.NOT_FLOODGATE_DATA, null));
            return;
        } catch (UnsupportedVersionException versionException) {
            disablePacketQueue(true);
            setKickMessage(Component.text(versionException.getMessage()));
            return;
        } catch (Exception exception) {
            if (logger.isDebug()) {
                logger.error("Exception while handling connection", exception);
            }
            disablePacketQueue(new HandleResult(HandleResultType.DECRYPT_ERROR, null));
            return;
        }

        if (seekResult.connection() == null) {
            // not a Floodgate player, make sure to resend the cancelled handshake packet
            disablePacketQueue(true);
            return;
        }

        blocker.enable();

        handshakeHandler
                .handleConnection(seekResult.connection())
                .thenAccept(result -> {
                    var connection = result.connection();

                    // we'll change the IP address to the real IP of the client very early on
                    // so that almost every plugin will use the real IP of the client
                    var port = ((InetSocketAddress) channel.remoteAddress()).getPort();
                    setNewIp(channel, new InetSocketAddress(connection.ip(), port));

                    this.handshakePacket = setHostname(handshakePacket, seekResult.dataRemainder());

                    if (result.shouldDisconnect()) {
                        setKickMessage(result.disconnectReason());
                    } else {
                        channel.attr(connectionAttribute).set(connection);
                    }

                    disablePacketQueue(new HandleResult(HandleResultType.SUCCESS, result));
                })
                .exceptionally(error -> {
                    logger.error("Unexpected error occurred", error);
                    return null;
                });
    }

    protected void disablePacketQueue(HandleResult result) {
        disablePacketQueue(shouldRemoveHandler(result));
    }

    protected void disablePacketQueue(boolean removeSelf) {
        if (handshakePacket != null && shouldCallFireRead(handshakePacket)) {
            ctx.fireChannelRead(handshakePacket);
        }

        if (removeSelf) {
            removeSelf();
        }
        Object queuedPacket;
        while ((queuedPacket = packetQueue.poll()) != null) {
            if (shouldCallFireRead(queuedPacket)) {
                ctx.fireChannelRead(queuedPacket);
            }
        }
        blocker.disable();
    }

    protected void removeSelf() {
        ctx.pipeline().remove(this);
    }

    protected final void setKickMessage(@NonNull Component message) {
        ctx.channel().attr(kickMessageAttribute).set(message);
    }

    protected final Component getKickMessage() {
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
        if (config.debug()) {
            cause.printStackTrace();
        }
    }
}
