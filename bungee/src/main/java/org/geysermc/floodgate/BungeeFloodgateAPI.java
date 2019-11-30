package org.geysermc.floodgate;

import net.md_5.bungee.api.connection.PendingConnection;

public class BungeeFloodgateAPI extends FloodgateAPI {
    public static FloodgatePlayer getPlayerByConnection(PendingConnection connection) {
        return getPlayer(connection.getUniqueId());
    }
}
