package org.geysermc.floodgate.api.link;

import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.UUID;

public interface LinkRequest {
    /**
     * Returns the Java username of the linked player.
     */
    String getJavaUsername();

    /**
     * Returns the Java unique id of the linked player.
     */
    UUID getJavaUniqueId();

    /**
     * Returns the code that the Bedrock player has to enter in order to link the account.
     */
    String getLinkCode();

    /**
     * Returns the username of player being linked.
     */
    String getBedrockUsername();

    /**
     * Returns the unix time when the player link was requested.
     */
    long getRequestTime();

    /**
     * If this player link request is expired.
     *
     * @param linkTimeout the link timeout in millis
     * @return true if the difference between now and requestTime is greater then the link timout
     */
    boolean isExpired(long linkTimeout);

    /**
     * Checks if the given FloodgatePlayer is the player requested in this LinkRequest.
     * This method will check both the real bedrock username
     * {@link FloodgatePlayer#getUsername()} and the edited username
     * {@link FloodgatePlayer#getJavaUsername()} and returns true if one of the two matches.
     *
     * @param player the player to check
     * @return true if the given player is the player requested
     */
    default boolean isRequestedPlayer(FloodgatePlayer player) {
        return getBedrockUsername().equals(player.getUsername()) ||
                getBedrockUsername().equals(player.getJavaUsername());
    }
}
