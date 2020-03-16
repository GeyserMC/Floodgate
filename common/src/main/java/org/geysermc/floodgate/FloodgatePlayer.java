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
     * The Unique Identifier of the Bedrock client, whether there is a linked Java account or not.
     */
    private UUID trueBedrockId;
    /**
     * The Unique Identifier of the Bedrock client, or the UUID of the linked Java account.
     */
    private UUID bedrockId;
    /**
     * The Java UUID used to identify the bedrock client
     */
    private UUID javaUniqueId; ///////////////////////////////////////////////////////////////////////
    /**
     * The operation system of the bedrock client
     */
    private String xuid; //////////////////////////////////////////////////
    private DeviceOS deviceOS;
    /**
     * The language code of the bedrock client
     */
    private String languageCode;
    /**
     * The LinkedPlayer object if the player is linked to Java account, or otherwise null.
     */
    private LinkedPlayer linkedplayer;

    FloodgatePlayer(BedrockData data) {
        xuid = data.getXuid(); //////////////////////////////////////////////////////////////////////////
        version = data.getVersion();
        username = data.getUsername();
        deviceOS = DeviceOS.getById(data.getDeviceId());
        languageCode = data.getLanguageCode();
        trueBedrockId = AbstractFloodgateAPI.createJavaPlayerId(Long.parseLong(data.getXuid())); // data.getBedrockId(); ////////////////////////////////////////////
        if (PlayerLink.enabled && PlayerLink.isLinkedPlayer(trueBedrockId)) { // If the account is linked to a Java account
            linkedplayer = PlayerLink.getLinkedPlayer(trueBedrockId);
            javaUsername = linkedplayer.javaUsername;
            bedrockId = linkedplayer.javaUniqueId;
            javaUniqueId = bedrockId;
        } else {
            javaUniqueId = AbstractFloodgateAPI.createJavaPlayerId(Long.parseLong(data.getXuid())); ////////////////////////////////////////////
            linkedplayer = null;
            javaUsername = "*" + data.getUsername().substring(0, Math.min(data.getUsername().length(), 15));
            bedrockId = trueBedrockId;
        }

    }
}
