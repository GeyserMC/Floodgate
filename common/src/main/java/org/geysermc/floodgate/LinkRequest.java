package org.geysermc.floodgate;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

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
      return timePassed > PlayerLink.getVerifyLinkTimeout();
    }

    public boolean checkGamerTag(FloodgatePlayer player) {
      if (bedrockUsername.equals(player.username) || bedrockUsername.equals(player.javaUsername)) { // Accept the request whether the prefix was used or not
        return true;
      } else {
        return false;
      }
    }
}
