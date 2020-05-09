package org.geysermc.floodgate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

abstract class AbstractFloodgateAPI {
    static final Map<UUID, FloodgatePlayer> players = new HashMap<>();

    /**
     * Get info about the given Bedrock player
     * @param uuid the uuid of the <b>online</b> Bedrock player
     * @return FloodgatePlayer if the given uuid is a Bedrock player
     */
    public static FloodgatePlayer getPlayer(UUID uuid) {
        FloodgatePlayer player = players.get(uuid);
        if (player != null || isFloodgateId(uuid)) return player;
        // make it possible to find player by Java id (for example for a linked player)
        for (FloodgatePlayer player1 : players.values()) {
            if (player1.getCorrectUniqueId().equals(uuid)) {
                return player1;
            }
        }
        return null;
    }

    /**
     * Removes a player (should only be used internally)
     * @param onlineId The UUID of the online player
     * @param removeLogin true if it should remove a sessions who is still logging in
     * @return true if player was a LinkedPlayer
     */
    static boolean removePlayer(UUID onlineId, boolean removeLogin) {
        FloodgatePlayer player = players.remove(onlineId);
        // LinkedPlayer is never registered under his java id
        // and a linked player has never a FloodgateId
        if (player != null || isFloodgateId(onlineId)) return false;

        for (FloodgatePlayer player1 : players.values()) {
            if (player1.isLogin() && !removeLogin || !player1.isLogin() && removeLogin) continue;
            if (!player1.getCorrectUniqueId().equals(onlineId)) continue;
            players.remove(player1.getJavaUniqueId());
            return true;
        }
        return false;
    }

    /**
     * {@link #removePlayer(UUID, boolean)} but with removeLogin on false
     */
    static boolean removePlayer(UUID onlineId) {
        return removePlayer(onlineId, false);
    }

    /**
     * Method to determine if the given <b>online</b> player is a bedrock player
     * @param uuid The uuid of the <b>online</b> player
     * @return true if the given <b>online</b> player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        return getPlayer(uuid) != null;
    }

    /**
     * Create a valid Java player uuid of a xuid
     */
    public static UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }

    public static boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }
}
