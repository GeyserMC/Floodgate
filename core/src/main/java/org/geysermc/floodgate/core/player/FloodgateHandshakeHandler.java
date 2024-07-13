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

package org.geysermc.floodgate.core.player;

import static org.geysermc.floodgate.core.player.FloodgateHandshakeHandler.ResultType.INVALID_DATA_LENGTH;
import static org.geysermc.floodgate.core.player.FloodgateHandshakeHandler.ResultType.NOT_FLOODGATE_DATA;
import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;

import com.google.common.base.Charsets;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.geysermc.floodgate.core.addon.data.HandshakeDataImpl;
import org.geysermc.floodgate.core.addon.data.HandshakeHandlersImpl;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.core.skin.SkinUploadManager;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.InvalidFormatException;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.core.util.Utils;

public final class FloodgateHandshakeHandler {
    private final HandshakeHandlersImpl handshakeHandlers;
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private final FloodgateConfig config;
    private final SkinUploadManager skinUploadManager;
    private final AttributeKey<FloodgatePlayer> playerAttribute;
    private final FloodgateLogger logger;
    private final LanguageManager languageManager;

    public FloodgateHandshakeHandler(
            HandshakeHandlersImpl handshakeHandlers,
            SimpleFloodgateApi api,
            FloodgateCipher cipher,
            FloodgateConfig config,
            SkinUploadManager skinUploadManager,
            AttributeKey<FloodgatePlayer> playerAttribute,
            FloodgateLogger logger,
            LanguageManager languageManager) {

        this.handshakeHandlers = handshakeHandlers;
        this.api = api;
        this.cipher = cipher;
        this.config = config;
        this.skinUploadManager = skinUploadManager;
        this.playerAttribute = playerAttribute;
        this.logger = logger;
        this.languageManager = languageManager;
    }

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
            int version = FloodgateCipher.version(value);
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
            @NonNull String hostname) {

        byte[] floodgateData = floodgateDataString.getBytes(Charsets.UTF_8);

        return CompletableFuture.supplyAsync(() -> {

            String decrypted;
            try {
                // the actual decryption of the data
                decrypted = cipher.decryptToString(floodgateData);
            } catch (InvalidFormatException e) {
                // when the Floodgate format couldn't be found
                throw callHandlerAndReturnResult(
                        NOT_FLOODGATE_DATA,
                        channel, null, hostname
                );
            } catch (Exception e) {
                // all the other exceptions are caused by invalid/tempered Floodgate data
                if (config.isDebug()) {
                    e.printStackTrace();
                }

                throw callHandlerAndReturnResult(
                        ResultType.DECRYPT_ERROR,
                        channel, null, hostname
                );
            }

            try {
                BedrockData bedrockData = BedrockData.fromString(decrypted);

                if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                    throw callHandlerAndReturnResult(
                            INVALID_DATA_LENGTH,
                            channel, bedrockData, hostname
                    );
                }

                // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
                if (bedrockData.hasPlayerLink()) {
                    throw handlePart2(channel, hostname, bedrockData, bedrockData.getLinkedPlayer());
                }
                //todo add option to not check for links when the data comes from a proxy

                // let's check if there is a link
                return bedrockData;

            } catch (Exception exception) {
                if (exception instanceof HandshakeResult) {
                    throw (HandshakeResult) exception;
                }
                exception.printStackTrace();

                throw callHandlerAndReturnResult(
                        ResultType.EXCEPTION,
                        channel, null, hostname
                );
            }
        }).thenCompose(this::fetchLinkedPlayer).handle((result, error) -> {
            if (error == null) {
                return handlePart2(channel, hostname, result.left(), result.right());
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

            return callHandlerAndReturnResult(
                    ResultType.EXCEPTION,
                    channel, null, hostname
            );
        });
    }

    private HandshakeResult handlePart2(
            Channel channel,
            String hostname,
            BedrockData bedrockData,
            LinkedPlayer linkedPlayer) {

        try {
            HandshakeData handshakeData = new HandshakeDataImpl(
                    channel, true, bedrockData.clone(), config,
                    linkedPlayer != null ? linkedPlayer.clone() : null, hostname);

            if (config.getPlayerLink().isRequireLink() && linkedPlayer == null) {
                String reason = languageManager.getString(
                        "floodgate.core.not_linked",
                        bedrockData.getLanguageCode(),
                        Constants.LINK_INFO_URL
                );
                handshakeData.setDisconnectReason(reason);
            }

            handshakeHandlers.callHandshakeHandlers(handshakeData);

            if (!handshakeData.shouldDisconnect()) {
                skinUploadManager.addConnectionIfNeeded(bedrockData.getSubscribeId(),
                        bedrockData.getVerifyCode());
            }

            FloodgatePlayer player = FloodgatePlayerImpl.from(bedrockData, handshakeData);

            api.addPlayer(player);

            channel.attr(playerAttribute).set(player);

            int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
            InetSocketAddress socketAddress = new InetSocketAddress(handshakeData.getIp(), port);
            player.addProperty(PropertyKey.SOCKET_ADDRESS, socketAddress);

            return new HandshakeResult(ResultType.SUCCESS, handshakeData, bedrockData, player);
        } catch (Exception exception) {
            exception.printStackTrace();
            return callHandlerAndReturnResult(ResultType.EXCEPTION, channel, null, hostname);
        }
    }

    private HandshakeResult callHandlerAndReturnResult(
            ResultType resultType,
            Channel channel,
            BedrockData bedrockData,
            String hostname) {

        HandshakeData handshakeData = new HandshakeDataImpl(channel, bedrockData != null,
                bedrockData, config, null, hostname);
        handshakeHandlers.callHandshakeHandlers(handshakeData);

        return new HandshakeResult(resultType, handshakeData, bedrockData, null);
    }

    private CompletableFuture<Pair<BedrockData, LinkedPlayer>> fetchLinkedPlayer(BedrockData data) {
        if (!api.getPlayerLink().isEnabled()) {
            return CompletableFuture.completedFuture(new ObjectObjectImmutablePair<>(data, null));
        }
        return api.getPlayerLink().getLinkedPlayer(Utils.getJavaUuid(data.getXuid()))
                .thenApply(link -> new ObjectObjectImmutablePair<>(data, link))
                .handle((result, error) -> {
                    if (error != null) {
                        logger.error("The player linking implementation returned an error",
                                error.getCause());
                        return new ObjectObjectImmutablePair<>(data, null);
                    }
                    return result;
                });
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        DECRYPT_ERROR,
        INVALID_DATA_LENGTH,
        SUCCESS
    }

    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static class HandshakeResult extends IllegalStateException {
        private final ResultType resultType;
        private final HandshakeData handshakeData;
        private final BedrockData bedrockData;
        private final FloodgatePlayer floodgatePlayer;

        public InetSocketAddress getNewIp(Channel channel) {
            if (floodgatePlayer != null) {
                return floodgatePlayer.getProperty(PropertyKey.SOCKET_ADDRESS);
            }
            if (handshakeData.getIp() != null) {
                int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
                return new InetSocketAddress(handshakeData.getIp(), port);
            }
            return null;
        }
    }
}
