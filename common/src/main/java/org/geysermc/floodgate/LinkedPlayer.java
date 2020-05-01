package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter @Setter
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
}
