package org.geysermc.floodgate;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class FloodgateAPI {
    static final Map<UUID, FloodgatePlayer> players = new HashMap<>();

    public static FloodgatePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public static UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }

    public enum DeviceOS {
        @JsonEnumDefaultValue
        UNKOWN,
        ANDROID,
        IOS,
        OSX,
        FIREOS,
        GEARVR,
        HOLOLENS,
        WIN10,
        WIN32,
        DEDICATED,
        ORBIS,
        NX;

        private static final DeviceOS[] VALUES = values();

        public static DeviceOS getById(int id) {
            return id < VALUES.length ? VALUES[id] : VALUES[0];
        }
    }
}
