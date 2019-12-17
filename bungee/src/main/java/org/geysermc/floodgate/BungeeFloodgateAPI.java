package org.geysermc.floodgate;

import net.md_5.bungee.api.connection.PendingConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BungeeFloodgateAPI extends FloodgateAPI {
    private static Map<UUID, String> encryptedData = new HashMap<>();

    static void addEncryptedData(UUID uuid, String encryptedData) {
        BungeeFloodgateAPI.encryptedData.put(uuid, encryptedData); // just override it I guess
    }

    static void removeEncryptedData(UUID uuid) {
        encryptedData.remove(uuid);
    }

    static String getEncryptedData(UUID uuid) {
        return encryptedData.get(uuid);
    }

    /**
     * See {@link FloodgateAPI#getPlayer(UUID)}
     */
    public static FloodgatePlayer getPlayerByConnection(PendingConnection connection) {
        return getPlayer(connection.getUniqueId());
    }
}
