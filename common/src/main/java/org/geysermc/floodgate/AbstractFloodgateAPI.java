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
        FloodgatePlayer player = players.get(onlineId);
        // the player is a non-linked player or a linked player but somehow someone tried to
        // remove the player by his xuid, we have to find out
        if (player != null) {
            // we don't allow them to remove a player by his xuid
            // because a linked player is never registered by his linked java uuid
            if (player.getLinkedPlayer() != null) return false;

            // removeLogin logics
            if (player.isLogin() && !removeLogin || !player.isLogin() && removeLogin) {
                return false;
            }

            // passed the test
            players.remove(onlineId);
            // was the account linked?
            return player.getLinkedPlayer() != null;
        }

        // we still want to be able to remove a linked-player by his linked java uuid
        for (FloodgatePlayer player1 : players.values()) {
            if (player1.isLogin() && !removeLogin || !player1.isLogin() && removeLogin) continue;
            if (!player1.getCorrectUniqueId().equals(onlineId)) continue;
            players.remove(player1.getXuid());
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

    static boolean removePlayer(FloodgatePlayer player) {
        boolean removed = players.remove(player.getXuid(), player);
        return removed && player.getLinkedPlayer() != null;
    }

    /**
     * Method to determine if the given <b>online</b> player is a bedrock player
     * @param uuid The uuid of the <b>online</b> player
     * @return true if the given <b>online</b> player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        return getPlayer(uuid) != null;
    }

    public static boolean isFloodgateId(UUID uuid) {
        // Bedrock UUIDs, used by Geyser as XUID are version 3
        // Java UUIDs are version 4
        return uuid.version() == 3;
    }
}
