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
        return players.get(uuid);
    }

    /**
     * Method to determine if the given online player is a Bedrock player
     * @param uuid The uuid of the player
     * @return true if the given player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        // return uuid.version() == 3;
        return players.containsKey(uuid);
    }

//    /**
//     * Method to determine if the given player is a Java player
//     * @param uuid The uuid of the player
//     * @return true if the given player is a Bedrock player
//     */
//    public static boolean isJavaPlayer(UUID uuid) {
//        return uuid.version() == 4;
//    }

    public static UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }
}
