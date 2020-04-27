package org.geysermc.floodgate;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class LinkRequest {
    /**
     * The Java username of the linked player
     */
    private String javaUsername;
    /**
     * The Java UUID of the linked player
     */
    private UUID javaUniqueId;
    /**
     * The link code
     */
    private String linkCode;
    /**
     * The username of player being linked
     */
    private String bedrockUsername;
    /**
     * The time when the link was requested
     */
    private long unixTime;

    LinkRequest(String username, UUID uuid, String code, String beUsername) {
        javaUniqueId = uuid;
        javaUsername = username;
        linkCode = code;
        bedrockUsername = beUsername;
        unixTime = Instant.now().getEpochSecond();
    }

    public boolean isExpired() {
        long timePassed = Instant.now().getEpochSecond() - unixTime;
        return timePassed > PlayerLink.getVerifyLinkTimeout();
    }

    public boolean checkGamerTag(FloodgatePlayer player) {
        // Accept the request whether the prefix was used or not
        return bedrockUsername.equals(player.getUsername()) || bedrockUsername.equals(player.getJavaUsername());
    }
}
