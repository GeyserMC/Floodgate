package org.geysermc.floodgate;

import org.bukkit.entity.Player;

import java.util.UUID;

public class FloodgateAPI extends AbstractFloodgateAPI {
    /**
     * See {@link AbstractFloodgateAPI#getPlayer(UUID)}
     */
    public static FloodgatePlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * See {@link AbstractFloodgateAPI#isBedrockPlayer(UUID)}
     */
    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }
}
