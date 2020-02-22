package org.geysermc.floodgate;

import com.velocitypowered.api.proxy.Player;

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

    public static String getEncryptedData(UUID uuid) {
        return encryptedData.get(uuid);
    }

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
