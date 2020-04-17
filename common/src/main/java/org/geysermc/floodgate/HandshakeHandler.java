package org.geysermc.floodgate;

import lombok.*;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.EncryptionUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import static org.geysermc.floodgate.util.BedrockData.EXPECTED_LENGTH;
import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;

public class HandshakeHandler {
    private PrivateKey privateKey;
    private boolean bungee;
    private String usernamePrefix;
    private boolean replaceSpaces;

    public HandshakeHandler(@NonNull PrivateKey privateKey, boolean bungee, String usernamePrefix, boolean replaceSpaces) {
        this.privateKey = privateKey;
        this.bungee = bungee;
        this.usernamePrefix = usernamePrefix;
        this.replaceSpaces = replaceSpaces;
    }

    public HandshakeResult handle(@NonNull String handshakeData) {
        try {
            String[] data = handshakeData.split("\0");
            boolean isBungeeData = data.length == 6 || data.length == 7;

            if (bungee && isBungeeData || !isBungeeData && data.length != 4 || !data[1].equals(FLOODGATE_IDENTIFIER)) {
                return ResultType.NOT_FLOODGATE_DATA.getCachedResult();
            }

            BedrockData bedrockData = EncryptionUtil.decryptBedrockData(
                    privateKey, data[2] + '\0' + data[3]
            );

            if (bedrockData.getDataLength() != EXPECTED_LENGTH) {
                return ResultType.INVALID_DATA_LENGTH.getCachedResult();
            }

            FloodgatePlayer player = new FloodgatePlayer(bedrockData, usernamePrefix, replaceSpaces);
            AbstractFloodgateAPI.players.put(player.getJavaUniqueId(), player);

            return new HandshakeResult(ResultType.SUCCESS, data, bedrockData, player);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return ResultType.EXCEPTION.getCachedResult();
        }
    }

    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter @ToString
    public static class HandshakeResult {
        private ResultType resultType;
        private String[] handshakeData;
        private BedrockData bedrockData;
        private FloodgatePlayer floodgatePlayer;
    }

    public enum ResultType {
        EXCEPTION,
        NOT_FLOODGATE_DATA,
        INVALID_DATA_LENGTH,
        SUCCESS;

        @Getter private HandshakeResult cachedResult;

        ResultType() {
            cachedResult = new HandshakeResult(this, null, null, null);
        }
    }
}
