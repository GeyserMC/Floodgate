package org.geysermc.floodgate;

import lombok.Getter;

import java.util.UUID;
import java.time.Instant;

@Getter
public class LinkRequest {
    /**
     * The Java username of the linked player
     */
    public String javaUsername;
    /**
     * The Java UUID of the linked player
     */
    public UUID javaUniqueId;
    /**
     * The link code
     */
    public String linkCode;
    /**
     * The username of player being linked
     */
    public String bedrockUsername;
    /**
     * The time when the link was requested
     */
    public long unixTime;

    LinkRequest(String username, UUID uuid, String code, String beUsername) {
        javaUniqueId = uuid;
        javaUsername = username;
        linkCode = code;
        bedrockUsername = beUsername;
        unixTime = Instant.now().getEpochSecond();
    }

    public boolean isExpired() {
      long timePassed = Instant.now().getEpochSecond() - unixTime;
      // System.out.println("Time passed: " + timePassed);
      return timePassed > PlayerLink.linkCodeTimeout;
    }
}
