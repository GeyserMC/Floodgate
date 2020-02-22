package org.geysermc.floodgate;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FloodgateAPI extends AbstractFloodgateAPI {
    private static Map<UUID, String> encryptedData = new HashMap<>();

    static void addEncryptedData(UUID uuid, String encryptedData) {
        FloodgateAPI.encryptedData.put(uuid, encryptedData); // just override it I guess
    }

    static void removeEncryptedData(UUID uuid) {
        encryptedData.remove(uuid);
    }

    static String getEncryptedData(UUID uuid) {
        return encryptedData.get(uuid);
    }

    /**
     * See {@link AbstractFloodgateAPI#getPlayer(UUID)}
     */
    public static FloodgatePlayer getPlayerByConnection(PendingConnection connection) {
        return getPlayer(connection.getUniqueId());
    }

    public static boolean isBedrockPlayer(ProxiedPlayer player) {
        return isBedrockPlayer(player.getUniqueId());
    }
}
