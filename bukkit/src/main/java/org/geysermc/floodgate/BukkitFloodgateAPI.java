package org.geysermc.floodgate;

import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitFloodgateAPI extends FloodgateAPI {
    /**
     * See {@link FloodgateAPI#getPlayer(UUID)}
     */
    public static FloodgatePlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }
}
