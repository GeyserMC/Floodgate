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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.util.Base64Utils;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.InvalidFormatException;
import org.geysermc.floodgate.util.RawSkin;

@RequiredArgsConstructor
public final class HandshakeHandler {
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private final FloodgateConfigHolder configHolder;
    private final AttributeKey<FloodgatePlayer> playerAttribute;

    public HandshakeResult handle(Channel channel, @NonNull String handshakeData) {
        try {
            String[] dataArray = handshakeData.split("\0");

            String data = null;
            for (String value : dataArray) {
                if (FloodgateCipher.hasHeader(value)) {
                    data = value;
                    break;
                }
            }

            if (data == null) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            // header + base64 iv - 0x21 - encrypted data - 0x21 - RawSkin
            int expectedHeaderLength = FloodgateCipher.HEADER_LENGTH +
                    Base64Utils.getEncodedLength(AesCipher.IV_LENGTH);
            int lastSplitIndex = data.lastIndexOf(0x21);

            byte[] floodgateData;
            byte[] rawSkinData = null;

            // if it has a RawSkin
            if (lastSplitIndex - expectedHeaderLength > 0) {
                floodgateData = data.substring(0, lastSplitIndex).getBytes(Charsets.UTF_8);
                rawSkinData = data.substring(lastSplitIndex + 1).getBytes(Charsets.UTF_8);
            } else {
                floodgateData = data.getBytes(Charsets.UTF_8);
            }

            // actual decryption
            String decrypted = cipher.decryptToString(floodgateData);
            BedrockData bedrockData = BedrockData.fromString(decrypted);

            if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                return ResultType.INVALID_DATA_LENGTH.getCachedResult();
            }

            RawSkin rawSkin = null;
            // only decompile the skin after knowing that the floodgateData is legit
            // note that we don't store a hash or anything in the BedrockData,
            // so a mitm can change skins
            if (rawSkinData != null) {
                rawSkin = RawSkin.decode(rawSkinData);
            }

            FloodgatePlayer player = FloodgatePlayerImpl.from(bedrockData, rawSkin, configHolder);
            api.addPlayer(player.getJavaUniqueId(), player);

            channel.attr(playerAttribute).set(player);

            int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
            InetSocketAddress socketAddress = new InetSocketAddress(bedrockData.getIp(), port);
            player.addProperty(PropertyKey.SOCKET_ADDRESS, socketAddress);

            return new HandshakeResult(ResultType.SUCCESS, dataArray, bedrockData, player);
        } catch (InvalidFormatException formatException) {
            // only header exceptions should return 'not floodgate data',
            // all the other format exceptions are because of invalid/tempered Floodgate data
            if (formatException.isHeader()) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            formatException.printStackTrace();
            return ResultType.EXCEPTION.getCachedResult();
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResultType.EXCEPTION.getCachedResult();
        }
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        INVALID_DATA_LENGTH,
        SUCCESS;

        @Getter
        private final HandshakeResult cachedResult;

        ResultType() {
            cachedResult = new HandshakeResult(this, null, null, null);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static class HandshakeResult {
        private final ResultType resultType;
        private final String[] handshakeData;
        private final BedrockData bedrockData;
        private final FloodgatePlayer floodgatePlayer;
    }
}
