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
     * Bedrock username with > identifier
     */
    private String javaUsername;
    /**
     * The Unique Identifier of the Bedrock client.
     */
    private UUID bedrockId;
    /**
     * The operation system of the bedrock client
     */
    private DeviceOS deviceOS;
    /**
     * The language code of the bedrock client
     */
    private String languageCode;

    FloodgatePlayer(BedrockData data) {
        version = data.getVersion();
        username = data.getUsername();
        javaUsername = "*" + data.getUsername().substring(0, Math.min(data.getUsername().length(), 15));
        bedrockId = data.getBedrockId();
        deviceOS = DeviceOS.getById(data.getDeviceId());
        languageCode = data.getLanguageCode();
    }
}
