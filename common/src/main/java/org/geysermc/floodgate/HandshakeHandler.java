/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate;

import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;

import com.google.common.base.Charsets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.InvalidFormatException;
import org.geysermc.floodgate.util.RawSkin;

@RequiredArgsConstructor
public final class HandshakeHandler {
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private final FloodgateConfigHolder configHolder;

    public HandshakeResult handle(@NonNull String handshakeData) {
        try {
            String[] dataArray = handshakeData.split("\0");

            boolean isBungeeData = dataArray.length == 5;
            // this can be Bungee data (without skin) or Floodgate data
            if (dataArray.length == 4) {
                isBungeeData = FloodgateCipher.hasHeader(dataArray[3]);
            }

            boolean proxy = configHolder.get().isProxy();
            //todo remove this check
            if (proxy && isBungeeData || !isBungeeData && dataArray.length != 2) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            // calculate the expected Base64 encoded IV length.
            int expectedIvLength = 4 * ((AesCipher.IV_LENGTH + 2) / 3);
            int lastSplitIndex = dataArray[1].lastIndexOf(0x21);

            byte[] floodgateData;
            byte[] rawSkinData = null;

            // if it has a RawSkin
            if (lastSplitIndex - expectedIvLength != 0) {
                floodgateData = dataArray[1].substring(0, lastSplitIndex).getBytes(Charsets.UTF_8);
                rawSkinData = dataArray[1].substring(lastSplitIndex + 1).getBytes(Charsets.UTF_8);
            } else {
                floodgateData = dataArray[1].getBytes(Charsets.UTF_8);
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

            System.out.println(rawSkin);

            FloodgatePlayer player = FloodgatePlayerImpl.from(bedrockData, rawSkin, configHolder);
            api.addPlayer(player.getJavaUniqueId(), player);

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

        public boolean isBungeeData() {
            return handshakeData.length == 4 || handshakeData.length == 5;
        }
    }
}
