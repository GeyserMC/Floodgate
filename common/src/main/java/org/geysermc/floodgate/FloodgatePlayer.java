package org.geysermc.floodgate;

import lombok.Getter;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.DeviceOS;

import java.util.UUID;

@Getter
public class FloodgatePlayer {
    /**
     * Bedrock version of the client
     */
    private String version;
    /**
     * Bedrock username (full version)
     */
    private String username;
    /**
     * Bedrock username with the given identifier<br>
     * This won't be null if it is an {@link LinkedPlayer LinkedPlayer}, but it isn't used
     */
    private String javaUsername;
    /**
     * The Unique Identifier used at the server to identify the bedrock client.<br>
     * Note that this field is only used when the player is not an {@link LinkedPlayer LinkedPlayer}
     */
    private UUID javaUniqueId;
    /**
     * The Xbox Unique Identifier
     */
    private String xuid;
    /**
     * The operation system of the bedrock client
     */
    private DeviceOS deviceOS;
    /**
     * The language code of the bedrock client
     */
    private String languageCode;
    /**
     * The InputMode of the bedrock client
     */
    private int inputMode;
    /**
     * The LinkedPlayer object if the player is linked to Java account, or otherwise null.
     */
    private LinkedPlayer linkedPlayer;

    FloodgatePlayer(BedrockData data) {
        xuid = data.getXuid();
        version = data.getVersion();
        username = data.getUsername();
        deviceOS = DeviceOS.getById(data.getDeviceId());
        languageCode = data.getLanguageCode();
        inputMode = data.getInputMode();
        javaUniqueId = AbstractFloodgateAPI.createJavaPlayerId(Long.parseLong(data.getXuid()));
        javaUsername = "*" + data.getUsername().substring(0, Math.min(data.getUsername().length(), 15));
        if (PlayerLink.isEnabledAndAllowed()) {
            linkedPlayer = PlayerLink.getInstance().getLinkedPlayer(javaUniqueId); //todo change to bedrockId once fixed
        }
    }

    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.javaUniqueId : javaUniqueId;
    }

    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.javaUsername : javaUsername;
    }
}
