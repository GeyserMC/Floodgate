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

package org.geysermc.floodgate.player;

import static org.geysermc.floodgate.player.FloodgateHandshakeHandler.ResultType.INVALID_DATA_LENGTH;
import static org.geysermc.floodgate.player.FloodgateHandshakeHandler.ResultType.NOT_FLOODGATE_DATA;
import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.addon.data.HandshakeDataImpl;
import org.geysermc.floodgate.addon.data.HandshakeHandlersImpl;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.skin.SkinUploadManager;
import org.geysermc.floodgate.time.TimeSyncer;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.InvalidFormatException;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.TimeSyncerHolder;
import org.geysermc.floodgate.util.Utils;

@RequiredArgsConstructor
public final class FloodgateHandshakeHandler {
    private final Cache<String, Long> handleCache =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .build();

    private final HandshakeHandlersImpl handshakeHandlers;
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private final FloodgateConfigHolder configHolder;
    private final SkinUploadManager skinUploadManager;
    private final AttributeKey<FloodgatePlayer> playerAttribute;
    private final FloodgateLogger logger;

    public CompletableFuture<HandshakeResult> handle(
            @NonNull Channel channel,
            @NonNull String originalHostname) {

        String[] split = originalHostname.split("\0");
        String data = null;

        StringBuilder hostnameBuilder = new StringBuilder();
        for (String value : split) {
            if (data == null && FloodgateCipher.hasHeader(value)) {
                data = value;
                continue;
            }
            hostnameBuilder.append(value).append('\0');
        }
        // hostname now doesn't have Floodgate data anymore if it had
        String hostname = hostnameBuilder.toString();

        if (data == null) {
            return CompletableFuture.completedFuture(
                    callHandlerAndReturnResult(NOT_FLOODGATE_DATA, channel, null, hostname)
            );
        }

        byte[] floodgateData = data.getBytes(Charsets.UTF_8);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // actual decryption
                String decrypted = cipher.decryptToString(floodgateData);
                BedrockData bedrockData = BedrockData.fromString(decrypted);

                if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                    throw callHandlerAndReturnResult(
                            INVALID_DATA_LENGTH,
                            channel, bedrockData, hostname
                    );
                }

                // timestamp checks

                TimeSyncer timeSyncer = TimeSyncerHolder.get();

                if (!timeSyncer.hasUsefulOffset()) {
                    logger.warn("We couldn't make sure that your system clock is accurate. " +
                            "This can cause issues with logging in.");
                }

                // the time syncer is accurate, but we have to account for some minor differences
                final int errorMargin = 150; // 150ms

                long timeDifference = timeSyncer.getRealMillis() - bedrockData.getTimestamp();
                if (timeDifference > 6000 + errorMargin || timeDifference < -errorMargin) {
                    if (Constants.DEBUG_MODE || logger.isDebug()) {
                        logger.info("Current time: " + System.currentTimeMillis());
                        logger.info("Stored time: " + bedrockData.getTimestamp());
                        logger.info("Time offset: " + timeSyncer.getTimeOffset());
                    }
                    throw callHandlerAndReturnResult(
                            ResultType.TIMESTAMP_DENIED,
                            channel, bedrockData, hostname
                    );
                }

                Long cachedTimestamp = handleCache.getIfPresent(bedrockData.getXuid());
                if (cachedTimestamp != null) {
                    // the cached timestamp should be older than the received timestamp
                    // and it should also not be possible to reuse the handshake
                    long diff = bedrockData.getTimestamp() - cachedTimestamp;
                    if (diff == 0 || diff < 0 && -diff > errorMargin) {
                        throw callHandlerAndReturnResult(
                                ResultType.TIMESTAMP_DENIED,
                                channel, bedrockData, hostname
                        );
                    }
                }

                handleCache.put(bedrockData.getXuid(), bedrockData.getTimestamp());

                // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
                if (bedrockData.hasPlayerLink()) {
                    throw handlePart2(channel, hostname, bedrockData, bedrockData.getLinkedPlayer());
                }
                //todo add option to not check for links when the data comes from a proxy

                // let's check if there is a link
                return bedrockData;

            } catch (InvalidFormatException formatException) {
                // only header exceptions should return 'not floodgate data',
                // all the other format exceptions are because of invalid/tempered Floodgate data
                if (formatException.isHeader()) {
                    throw callHandlerAndReturnResult(
                            NOT_FLOODGATE_DATA,
                            channel, null, hostname
                    );
                }

                formatException.printStackTrace();

                throw callHandlerAndReturnResult(
                        ResultType.EXCEPTION,
                        channel, null, hostname
                );
            } catch (Exception exception) {
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

            if (error instanceof HandshakeResult) {
                return (HandshakeResult) error;
            }

            return callHandlerAndReturnResult(
                    ResultType.EXCEPTION,
                    channel, null, hostname
            );
        });
    }

    private HandshakeResult handlePart2(Channel channel, String hostname, BedrockData bedrockData, LinkedPlayer linkedPlayer) {
        try {
            HandshakeData handshakeData = new HandshakeDataImpl(
                    channel, true, bedrockData.clone(), configHolder.get(),
                    linkedPlayer != null ? linkedPlayer.clone() : null, hostname);

            if (configHolder.get().getPlayerLink().isRequireLink() && linkedPlayer == null) {
                handshakeData.setDisconnectReason("floodgate.core.not_linked");
            }

            handshakeHandlers.callHandshakeHandlers(handshakeData);

            if (!handshakeData.shouldDisconnect()) {
                skinUploadManager.addConnectionIfNeeded(bedrockData.getSubscribeId(),
                        bedrockData.getVerifyCode());
            }

            correctHostname(handshakeData);

            FloodgatePlayer player = FloodgatePlayerImpl.from(bedrockData, handshakeData);

            api.addPlayer(player.getJavaUniqueId(), player);

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
                bedrockData, configHolder.get(), null, hostname);
        handshakeHandlers.callHandshakeHandlers(handshakeData);

        if (bedrockData != null) {
            correctHostname(handshakeData);
        }

        return new HandshakeResult(resultType, handshakeData, bedrockData, null);
    }

    private void correctHostname(HandshakeData handshakeData) {
        BedrockData bedrockData = handshakeData.getBedrockData();
        UUID correctUuid = handshakeData.getCorrectUniqueId();

        // replace the ip and uuid with the Bedrock client IP and an uuid based of the xuid
        String[] split = handshakeData.getHostname().split("\0");
        if (split.length >= 3) {
            if (logger.isDebug()) {
                logger.info("Replacing hostname arg1 '{}' with '{}' and arg2 '{}' with '{}'",
                        split[1], bedrockData.getIp(), split[2], correctUuid.toString());
            }
            split[1] = bedrockData.getIp();
            split[2] = correctUuid.toString();
        }
        handshakeData.setHostname(String.join("\0", split));
    }

    private CompletableFuture<Pair<BedrockData, LinkedPlayer>> fetchLinkedPlayer(BedrockData data) {
        if (!api.getPlayerLink().isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return api.getPlayerLink().getLinkedPlayer(Utils.getJavaUuid(data.getXuid()))
                .thenApply(link -> new ObjectObjectMutablePair<>(data, link));
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        INVALID_DATA_LENGTH,
        TIMESTAMP_DENIED,
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
