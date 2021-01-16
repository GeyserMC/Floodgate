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

package org.geysermc.floodgate.handler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.player.FloodgateHandshakeHandler.ResultType;

import com.google.inject.Inject;
import io.netty.channel.Channel;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtils;

public final class BungeeDataHandler {
    private static final Field EXTRA_HANDSHAKE_DATA;
    private static final Field PLAYER_NAME;
    private static final Field PLAYER_CHANNEL_WRAPPER;
    private static final Field PLAYER_CHANNEL;
    private static final Field PLAYER_REMOTE_ADDRESS;
    private static final Field CACHED_HANDSHAKE_PACKET;

    static {
        Class<?> initialHandler = ReflectionUtils.getPrefixedClass("connection.InitialHandler");
        EXTRA_HANDSHAKE_DATA = ReflectionUtils.getField(initialHandler, "extraDataInHandshake");
        checkNotNull(EXTRA_HANDSHAKE_DATA, "extraDataInHandshake field cannot be null");

        PLAYER_NAME = ReflectionUtils.getField(initialHandler, "name");
        checkNotNull(PLAYER_NAME, "Initial name field cannot be null");

        Class<?> channelWrapper = ReflectionUtils.getPrefixedClass("netty.ChannelWrapper");
        PLAYER_CHANNEL_WRAPPER = ReflectionUtils.getFieldOfType(initialHandler, channelWrapper);
        checkNotNull(PLAYER_CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");

        PLAYER_CHANNEL = ReflectionUtils.getFieldOfType(channelWrapper, Channel.class);
        checkNotNull(PLAYER_CHANNEL, "Channel field cannot be null");

        PLAYER_REMOTE_ADDRESS = ReflectionUtils.getFieldOfType(channelWrapper, SocketAddress.class);
        checkNotNull(PLAYER_REMOTE_ADDRESS, "Remote address field cannot be null");

        Class<?> handshakePacket = ReflectionUtils.getPrefixedClass("protocol.packet.Handshake");
        CACHED_HANDSHAKE_PACKET = ReflectionUtils.getFieldOfType(initialHandler, handshakePacket);
        checkNotNull(CACHED_HANDSHAKE_PACKET, "Cached handshake packet field cannot be null");
    }

    @Inject private Plugin plugin;
    @Inject private ProxyFloodgateConfig config;
    @Inject private ProxyFloodgateApi api;
    @Inject private FloodgateHandshakeHandler handler;
    @Inject private FloodgateLogger logger;

    public void handlePreLogin(PreLoginEvent event) {
        event.registerIntent(plugin);
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            PendingConnection connection = event.getConnection();

            String extraData = ReflectionUtils.getCastedValue(connection, EXTRA_HANDSHAKE_DATA);

            Object channelWrapper = ReflectionUtils.getValue(connection, PLAYER_CHANNEL_WRAPPER);
            Channel channel = ReflectionUtils.getCastedValue(channelWrapper, PLAYER_CHANNEL);

            HandshakeResult result = handler.handle(channel, extraData);
            HandshakeData handshakeData = result.getHandshakeData();

            if (handshakeData.getDisconnectReason() != null) {
                //noinspection ConstantConditions
                channel.close(); // todo disconnect with message
                return;
            }

            if (result.getResultType() == ResultType.EXCEPTION) {
                event.setCancelReason(config.getDisconnect().getInvalidKey());
                event.completeIntent(plugin);
                return;
            }

            if (result.getResultType() == ResultType.INVALID_DATA_LENGTH) {
                event.setCancelReason(TextComponent.fromLegacyText(String.format(
                        config.getDisconnect().getInvalidArgumentsLength(),
                        BedrockData.EXPECTED_LENGTH, result.getBedrockData().getDataLength()
                )));
                event.completeIntent(plugin);
                return;
            }

            // only continue when SUCCESS
            if (result.getResultType() != ResultType.SUCCESS) {
                event.completeIntent(plugin);
                return;
            }

            FloodgatePlayer player = result.getFloodgatePlayer();

            connection.setOnlineMode(false);
            connection.setUniqueId(player.getCorrectUniqueId());

            ReflectionUtils.setValue(connection, PLAYER_NAME, player.getCorrectUsername());

            SocketAddress remoteAddress =
                    ReflectionUtils.getCastedValue(channelWrapper, PLAYER_REMOTE_ADDRESS);

            if (!(remoteAddress instanceof InetSocketAddress)) {
                logger.info("Player {} doesn't use an InetSocketAddress, it uses {}. " +
                                "Ignoring the player, I guess.",
                        player.getUsername(), remoteAddress.getClass().getSimpleName()
                );
                event.setCancelled(true);
                event.setCancelReason(
                        new TextComponent("remoteAddress is not an InetSocketAddress!"));
                event.completeIntent(plugin);
                return;
            }

            InetSocketAddress correctAddress = player.getProperty(PropertyKey.SOCKET_ADDRESS);
            ReflectionUtils.setValue(channelWrapper, PLAYER_REMOTE_ADDRESS, correctAddress);

            event.completeIntent(plugin);
        });
    }

    public void handleServerConnect(ProxiedPlayer player) {
        // Passes the information through to the connecting server if enabled
        if (config.isSendFloodgateData() && api.isFloodgatePlayer(player.getUniqueId())) {
            Handshake handshake = ReflectionUtils.getCastedValue(
                    player.getPendingConnection(), CACHED_HANDSHAKE_PACKET
            );

            // Ensures that only the hostname remains,
            // this way it can't mess up the Floodgate data format
            String initialHostname = handshake.getHost().split("\0")[0];
            handshake.setHost(initialHostname + '\0' + api.getEncryptedData(player.getUniqueId()));

            // Bungeecord will add his data after our data
        }
    }
}
