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

package org.geysermc.floodgate;

import com.google.inject.Inject;
import lombok.*;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.InvalidHeaderException;

import java.util.Base64;

import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;

@RequiredArgsConstructor
public final class HandshakeHandler {
    private final SimpleFloodgateApi api;
    private final FloodgateCipher cipher;
    private boolean proxy;

    private String usernamePrefix;
    private boolean replaceSpaces;

    @Inject
    public void init(FloodgateConfig config) {
        this.proxy = config.isProxy();
        this.usernamePrefix = config.getUsernamePrefix();
        this.replaceSpaces = config.isReplaceSpaces();
    }

    public HandshakeResult handle(@NonNull String handshakeData) {
        try {
            String[] data = handshakeData.split("\0");

            boolean isBungeeData = data.length == 5;
            // this can be Bungee data (without skin) or Floodgate data
            if (data.length == 4) {
                isBungeeData = FloodgateCipher.hasHeader(data[3]);
            }

            if (proxy && isBungeeData || !isBungeeData && data.length != 2) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            String decrypted = cipher.decryptToString(Base64.getDecoder().decode(data[1]));
            BedrockData bedrockData = BedrockData.fromString(decrypted);

            if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                return ResultType.INVALID_DATA_LENGTH.getCachedResult();
            }

            FloodgatePlayer player =
                    new FloodgatePlayerImpl(bedrockData, usernamePrefix, replaceSpaces);
            api.addPlayer(player.getJavaUniqueId(), player);

            return new HandshakeResult(ResultType.SUCCESS, data, bedrockData, player);
        } catch (InvalidHeaderException headerException) {
            return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println(handshakeData);
            return ResultType.EXCEPTION.getCachedResult();
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
}
