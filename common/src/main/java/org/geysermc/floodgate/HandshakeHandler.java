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
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.EncryptionUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;
import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;

@RequiredArgsConstructor
public final class HandshakeHandler {
    private final SimpleFloodgateApi api;
    private PrivateKey privateKey;
    private boolean proxy;

    private String usernamePrefix;
    private boolean replaceSpaces;

    @Inject
    public void init(FloodgateConfig config) {
        this.privateKey = config.getPrivateKey();
        checkNotNull(privateKey, "Floodgate key cannot be null");
        this.proxy = config.isProxy();
        this.usernamePrefix = config.getUsernamePrefix();
        this.replaceSpaces = config.isReplaceSpaces();
    }

    public HandshakeResult handle(@NonNull String handshakeData) {
        try {
            System.out.println(handshakeData);
            String[] data = handshakeData.split("\0");
            System.out.println(Arrays.toString(data));
            boolean isBungeeData = data.length == 6 || data.length == 7;

            if (proxy && isBungeeData || !isBungeeData && data.length != 4
                    || !data[1].equals(FLOODGATE_IDENTIFIER)) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            BedrockData bedrockData = EncryptionUtil.decryptBedrockData(
                    privateKey, data[2] + '\0' + data[3], null
            );

            if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                return ResultType.INVALID_DATA_LENGTH.getCachedResult();
            }

            FloodgatePlayer player =
                    new FloodgatePlayerImpl(bedrockData, usernamePrefix, replaceSpaces);
            api.addPlayer(player.getJavaUniqueId(), player);

            return new HandshakeResult(ResultType.SUCCESS, data, bedrockData, player);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException exception) {
            exception.printStackTrace();
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
