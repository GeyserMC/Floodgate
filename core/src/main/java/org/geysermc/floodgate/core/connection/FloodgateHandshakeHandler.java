/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.connection;

import static org.geysermc.floodgate.core.connection.FloodgateHandshakeHandler.ResultType.INVALID_DATA;
import static org.geysermc.floodgate.core.connection.FloodgateHandshakeHandler.ResultType.NOT_FLOODGATE_DATA;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.addon.data.HandshakeDataImpl;
import org.geysermc.floodgate.core.addon.data.HandshakeHandlersImpl;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.codec.FloodgateConnectionCodec;
import org.geysermc.floodgate.core.crypto.FloodgateDataCodec;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.skin.SkinUploadManager;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.InvalidFormatException;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.core.util.Utils;
import org.geysermc.floodgate.util.LinkedPlayer;

@Singleton
public final class FloodgateHandshakeHandler {
    @Inject ConnectionManager connectionManager;
    @Inject HandshakeHandlersImpl handshakeHandlers;
    @Inject SimpleFloodgateApi api;
    @Inject CommonPlayerLink link;
    @Inject FloodgateConfig config;
    @Inject FloodgateDataCodec dataCodec;
    @Inject FloodgateConnectionCodec connectionCodec;
    @Inject SkinUploadManager skinUploadManager;
    @Inject
    @Named("playerAttribute")
    AttributeKey<Connection> playerAttribute;
    @Inject FloodgateLogger logger;
    @Inject LanguageManager languageManager;

    /**
     * Separates the Floodgate data from the hostname
     *
     * @param hostname the string to look in
     * @return The first string is the Floodgate data (or null if there isn't) and the second string
     * is the hostname without the Floodgate.
     */
    public HostnameSeparationResult separateHostname(@NonNull String hostname) {
        String[] hostnameItems = hostname.split("\0");
        String floodgateData = null;
        int dataVersion = -1;

        StringBuilder builder = new StringBuilder();
        for (String value : hostnameItems) {
            int version = FloodgateDataCodec.version(value);
            if (floodgateData == null && version != -1) {
                floodgateData = value;
                dataVersion = version;
                continue;
            }

            if (builder.length() > 0) {
                builder.append('\0');
            }
            builder.append(value);
        }
        // the new hostname doesn't have Floodgate data anymore, if it had Floodgate data.
        return new HostnameSeparationResult(floodgateData, dataVersion, builder.toString());
    }

    public CompletableFuture<HandshakeResult> handle(
            @NonNull Channel channel,
            @NonNull String floodgateDataString,
            @NonNull String hostname
    ) {
        byte[] floodgateData = floodgateDataString.getBytes(StandardCharsets.UTF_8);

        return CompletableFuture.supplyAsync(() -> {

            ByteBuffer decoded;
            try {
                // the actual decryption of the data
                decoded = dataCodec.decode(floodgateData);
            } catch (InvalidFormatException e) {
                // when the Floodgate format couldn't be found
                throw callHandlerAndReturnResult(NOT_FLOODGATE_DATA, channel, hostname);
            } catch (Exception exception) {
                // all the other exceptions are caused by invalid/tempered Floodgate data
                if (config.debug()) {
                    exception.printStackTrace();
                }

                throw callHandlerAndReturnResult(ResultType.DECRYPT_ERROR, channel, hostname);
            }

            FloodgateConnection connection;
            try {
                connection = connectionCodec.decode(decoded);
            } catch (Exception exception) {
                // todo probably add a format version as that's the most likely reason for this error
                if (config.debug()) {
                    exception.printStackTrace();
                }

                throw callHandlerAndReturnResult(INVALID_DATA, channel,  hostname);
            }

            try {
                // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
                if (connection.isLinked()) {
                    throw handlePart2(channel, hostname, connection);
                }
                //todo add option to not check for links when the data comes from a proxy

                // let's check if there is a link
                return connection;

            } catch (Exception exception) {
                if (exception instanceof HandshakeResult) {
                    throw (HandshakeResult) exception;
                }
                exception.printStackTrace();

                throw callHandlerAndReturnResult(ResultType.EXCEPTION, channel, hostname);
            }
        }).thenCompose(this::fetchLinkedPlayer).handle((result, error) -> {
            if (error == null) {
                return handlePart2(channel, hostname, result);
            }

            if (error instanceof CompletionException) {
                if (error.getCause() == null) {
                    error.printStackTrace();
                }
                error = error.getCause();
            }

            if (error instanceof HandshakeResult) {
                return (HandshakeResult) error;
            }

            error.printStackTrace();

            return callHandlerAndReturnResult(ResultType.EXCEPTION, channel, hostname);
        });
    }

    private HandshakeResult handlePart2(
            Channel channel,
            String hostname,
            FloodgateConnection connection
    ) {
        try {
            var handshakeData = new HandshakeDataImpl(channel, connection, hostname);

            if (config.playerLink().requireLink() && !connection.isLinked()) {
                String reason = languageManager.getString(
                        "floodgate.core.not_linked",
                        connection.languageCode(),
                        Constants.LINK_INFO_URL
                );
                handshakeData.setDisconnectReason(reason);
            }

            handshakeHandlers.callHandshakeHandlers(handshakeData);

//            if (!handshakeData.shouldDisconnect()) {
//                skinUploadManager.addConnectionIfNeeded(bedrockData.getSubscribeId(),
//                        bedrockData.getVerifyCode());
//            }

            connection = handshakeData.applyChanges(connection, hostname, config);

            connectionManager.addConnection(connection);
            channel.attr(playerAttribute).set(connection);

            return new HandshakeResult(ResultType.SUCCESS, handshakeData, connection);
        } catch (Exception exception) {
            exception.printStackTrace();
            return callHandlerAndReturnResult(ResultType.EXCEPTION, channel, hostname);
        }
    }

    private HandshakeResult callHandlerAndReturnResult(
            ResultType resultType,
            Channel channel,
            String hostname
    ) {
        HandshakeData handshakeData = new HandshakeDataImpl(channel, null, hostname);
        handshakeHandlers.callHandshakeHandlers(handshakeData);

        return new HandshakeResult(resultType, handshakeData, null);
    }

    private CompletableFuture<FloodgateConnection> fetchLinkedPlayer(FloodgateConnection data) {
        if (!link.isEnabled()) {
            return CompletableFuture.completedFuture(data);
        }
        return link.fetchLink(Utils.getJavaUuid(data.xuid()))
                .thenApply(link -> {
                    if (link == null) {
                        return null;
                    }
                    var linked = LinkedPlayer.of(link.javaUsername(), link.javaUniqueId(), link.bedrockId());
                    return new FloodgateConnectionBuilder(config).linkedPlayer(linked).build();
                })
                .handle((result, error) -> {
                    if (error != null) {
                        logger.error("The player linking implementation returned an error",
                                error.getCause());
                        return data;
                    }
                    return result;
                });
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        DECRYPT_ERROR,
        INVALID_DATA,
        SUCCESS
    }

    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static class HandshakeResult extends IllegalStateException {
        private final ResultType resultType;
        private final HandshakeData handshakeData;
        private final Connection floodgatePlayer;

        public InetSocketAddress getNewIp(Channel channel) {
//            if (floodgatePlayer != null) {
//                return floodgatePlayer.socketAddress();
//            }
            if (handshakeData.getIp() != null) {
                int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
                return new InetSocketAddress(handshakeData.getIp(), port);
            }
            return null;
        }
    }
}
