/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.handler;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.HandshakeHandler;
import org.geysermc.floodgate.HandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.HandshakeHandler.*;
import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;

public final class BungeeDataHandler {
    private static final Field EXTRA_HANDSHAKE_DATA;
    private static final Field PLAYER_NAME;
    private static final Field PLAYER_CHANNEL_WRAPPER;
    private static final Field PLAYER_REMOTE_ADDRESS;
    private static final Field CACHED_HANDSHAKE_PACKET;

    private final Plugin plugin;
    private final ProxyFloodgateConfig config;
    private final ProxyFloodgateApi api;
    private final HandshakeHandler handler;
    private final FloodgateLogger logger;

    public BungeeDataHandler(Plugin plugin, ProxyFloodgateConfig config, ProxyFloodgateApi api,
                             HandshakeHandler handshakeHandler, FloodgateLogger logger) {
        this.plugin = plugin;
        this.config = config;
        this.handler = handshakeHandler;
        this.api = api;
        this.logger = logger;
    }

    public void handle(PreLoginEvent event) {
        event.registerIntent(plugin);
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            String extraData = ReflectionUtil.getCastedValue(
                    event.getConnection(), EXTRA_HANDSHAKE_DATA
            );

            HandshakeResult result = handler.handle(extraData);
            switch (result.getResultType()) {
                case SUCCESS:
                    break;
                case EXCEPTION:
                    event.setCancelReason(config.getMessages().getInvalidKey());
                    break;
                case INVALID_DATA_LENGTH:
                    event.setCancelReason(String.format(
                            config.getMessages().getInvalidArgumentsLength(),
                            BedrockData.EXPECTED_LENGTH, result.getBedrockData().getDataLength()
                    ));
                    break;
            }

            // only continue when SUCCESS
            if (result.getResultType() != ResultType.SUCCESS) {
                event.completeIntent(plugin);
                return;
            }

            FloodgatePlayer player = result.getFloodgatePlayer();
            api.addEncryptedData(
                    player.getCorrectUniqueId(),
                    result.getHandshakeData()[2] + '\0' + result.getHandshakeData()[3]
            );

            event.getConnection().setOnlineMode(false);
            event.getConnection().setUniqueId(player.getCorrectUniqueId());

            ReflectionUtil.setValue(
                    event.getConnection(), PLAYER_NAME, player.getCorrectUsername()
            );

            Object channelWrapper =
                    ReflectionUtil.getValue(event.getConnection(), PLAYER_CHANNEL_WRAPPER);

            SocketAddress remoteAddress =
                    ReflectionUtil.getCastedValue(channelWrapper, PLAYER_REMOTE_ADDRESS);

            if (!(remoteAddress instanceof InetSocketAddress)) {
                logger.info("Player {} doesn't use an InetSocketAddress. " +
                                "It uses {}. Ignoring the player, I guess.",
                        player.getUsername(), remoteAddress.getClass().getSimpleName()
                );
            } else {
                int port = ((InetSocketAddress) remoteAddress).getPort();
                ReflectionUtil.setValue(
                        channelWrapper, PLAYER_REMOTE_ADDRESS,
                        new InetSocketAddress(result.getBedrockData().getIp(), port)
                );
            }
            event.completeIntent(plugin);
        });
    }

    public void handle(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        // Passes the information through to the connecting server if enabled
        if (config.isSendFloodgateData() && api.isBedrockPlayer(player.getUniqueId())) {
            Handshake handshake = ReflectionUtil.getCastedValue(
                    player.getPendingConnection(), CACHED_HANDSHAKE_PACKET
            );

            // Ensures that only the hostname remains,
            // this way it can't mess up the Floodgate data format
            String initialHostname = handshake.getHost().split("\0")[0];
            handshake.setHost(initialHostname + '\0' + FLOODGATE_IDENTIFIER + '\0' +
                    api.getEncryptedData(player.getUniqueId())
            );

            // Bungeecord will add his data after our data
        }
    }

    static {
        Class<?> initialHandler = ReflectionUtil.getPrefixedClass("connection.InitialHandler");
        EXTRA_HANDSHAKE_DATA = ReflectionUtil.getField(initialHandler, "extraDataInHandshake");
        checkNotNull(EXTRA_HANDSHAKE_DATA, "extraDataInHandshake field cannot be null");

        PLAYER_NAME = ReflectionUtil.getField(initialHandler, "name");
        checkNotNull(PLAYER_NAME, "Initial name field cannot be null");

        Class<?> channelWrapper = ReflectionUtil.getPrefixedClass("netty.ChannelWrapper");
        PLAYER_CHANNEL_WRAPPER = ReflectionUtil.getFieldOfType(initialHandler, channelWrapper);
        checkNotNull(PLAYER_CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");

        PLAYER_REMOTE_ADDRESS = ReflectionUtil.getFieldOfType(channelWrapper, SocketAddress.class);
        checkNotNull(PLAYER_REMOTE_ADDRESS, "Remote address field cannot be null");

        Class<?> handshakePacket = ReflectionUtil.getPrefixedClass("protocol.packet.Handshake");
        CACHED_HANDSHAKE_PACKET = ReflectionUtil.getFieldOfType(initialHandler, handshakePacket);
        checkNotNull(CACHED_HANDSHAKE_PACKET, "Cached handshake packet field cannot be null");
    }
}
