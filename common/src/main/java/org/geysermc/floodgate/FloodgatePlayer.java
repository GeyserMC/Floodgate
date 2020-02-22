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
     * The Java UUID used to identify the bedrock client
     */
    private UUID javaUniqueId;

    FloodgatePlayer(BedrockData data) {
        version = data.getVersion();
        username = data.getUsername();
        javaUsername = "*" + data.getUsername().substring(0, Math.min(data.getUsername().length(), 15));
        xuid = data.getXuid();
        deviceOS = DeviceOS.getById(data.getDeviceId());
        languageCode = data.getLanguageCode();
        javaUniqueId = AbstractFloodgateAPI.createJavaPlayerId(Long.parseLong(data.getXuid()));
    }
}
