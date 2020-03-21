package org.geysermc.floodgate;

import lombok.Getter;

import java.util.UUID;

@Getter
public class LinkedPlayer {
    /**
     * The Java username of the linked player
     */
    public String javaUsername;
    /**
     * The Java UUID of the linked player
     */
    public UUID javaUniqueId;
    /**
     * The UUID of the Bedrock player
     */
    public UUID bedrockId;

    LinkedPlayer(String username, UUID uuid, UUID bedrockId) {
        this.bedrockId = bedrockId;
        this.javaUniqueId = uuid;
        this.javaUsername = username;
    }
}
