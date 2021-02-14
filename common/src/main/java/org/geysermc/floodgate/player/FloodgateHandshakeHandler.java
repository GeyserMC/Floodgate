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

import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;

import com.google.common.base.Charsets;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
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
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.InvalidFormatException;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.Utils;

@RequiredArgsConstructor
public final class FloodgateHandshakeHandler {
    private final HandshakeHandlersImpl handshakeHandlers;
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private final FloodgateConfigHolder configHolder;
    private final SkinUploadManager skinUploadManager;
    private final AttributeKey<FloodgatePlayer> playerAttribute;
    private final FloodgateLogger logger;

    public HandshakeResult handle(Channel channel, @NonNull String originalHostname) {
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
            return callHandlerAndReturnResult(
                    ResultType.NOT_FLOODGATE_DATA,
                    channel, null, hostname);
        }

        try {
            byte[] floodgateData = data.getBytes(Charsets.UTF_8);

            // actual decryption
            String decrypted = cipher.decryptToString(floodgateData);
            BedrockData bedrockData = BedrockData.fromString(decrypted);

            if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                return callHandlerAndReturnResult(
                        ResultType.INVALID_DATA_LENGTH,
                        channel, bedrockData, hostname);
            }

            LinkedPlayer linkedPlayer;

            // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
            if (bedrockData.hasPlayerLink()) {
                linkedPlayer = bedrockData.getLinkedPlayer();
            } else {
                // every implementation (Bukkit, Bungee and Velocity) run this constructor async,
                // so we should be fine doing this synchronised.
                linkedPlayer = fetchLinkedPlayer(Utils.getJavaUuid(bedrockData.getXuid()));
            }

            HandshakeData handshakeData = new HandshakeDataImpl(
                    channel, true, bedrockData.clone(), configHolder.get(),
                    linkedPlayer != null ? linkedPlayer.clone() : null, hostname);
            handshakeHandlers.callHandshakeHandlers(handshakeData);

            if (!handshakeData.shouldDisconnect()) {
                skinUploadManager.addConnectionIfNeeded(bedrockData.getSubscribeId(),
                        bedrockData.getVerifyCode());
            }

            UUID javaUuid = Utils.getJavaUuid(bedrockData.getXuid());
            handshakeData.setHostname(correctHostname(
                    handshakeData.getHostname(), bedrockData, javaUuid
            ));

            FloodgatePlayer player =
                    FloodgatePlayerImpl.from(bedrockData, handshakeData);

            api.addPlayer(player.getJavaUniqueId(), player);

            channel.attr(playerAttribute).set(player);

            int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
            InetSocketAddress socketAddress = new InetSocketAddress(handshakeData.getBedrockIp(),
                    port);
            player.addProperty(PropertyKey.SOCKET_ADDRESS, socketAddress);

            return new HandshakeResult(ResultType.SUCCESS, handshakeData, bedrockData, player);

        } catch (InvalidFormatException formatException) {
            // only header exceptions should return 'not floodgate data',
            // all the other format exceptions are because of invalid/tempered Floodgate data
            if (formatException.isHeader()) {
                return callHandlerAndReturnResult(
                        ResultType.NOT_FLOODGATE_DATA,
                        channel, null, hostname);
            }

            formatException.printStackTrace();

            return callHandlerAndReturnResult(
                    ResultType.EXCEPTION,
                    channel, null, hostname);

        } catch (Exception exception) {
            exception.printStackTrace();

            return callHandlerAndReturnResult(
                    ResultType.EXCEPTION,
                    channel, null, hostname);
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
            UUID javaUuid = Utils.getJavaUuid(bedrockData.getXuid());
            handshakeData.setHostname(correctHostname(
                    handshakeData.getHostname(), bedrockData, javaUuid
            ));
        }

        return new HandshakeResult(resultType, handshakeData, bedrockData, null);
    }

    private String correctHostname(String hostname, BedrockData data, UUID correctUuid) {
        // replace the ip and uuid with the Bedrock client IP and an uuid based of the xuid
        String[] split = hostname.split("\0");
        if (split.length >= 3) {
            if (logger.isDebug()) {
                logger.info("Replacing hostname arg1 '{}' with '{}' and arg2 '{}' with '{}'",
                        split[1], data.getIp(), split[2], correctUuid.toString());
            }
            split[1] = data.getIp();
            split[2] = correctUuid.toString();
        }
        return String.join("\0", split);
    }

    private LinkedPlayer fetchLinkedPlayer(UUID javaUniqueId) {
        if (!api.getPlayerLink().isEnabled()) {
            return null;
        }

        try {
            return api.getPlayerLink().getLinkedPlayer(javaUniqueId).get();
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        INVALID_DATA_LENGTH,
        SUCCESS
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class HandshakeResult {
        private final ResultType resultType;
        private final HandshakeData handshakeData;
        private final BedrockData bedrockData;
        private final FloodgatePlayer floodgatePlayer;
    }
}
