package org.geysermc.floodgate.util;


import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum DeviceOS {
    @JsonEnumDefaultValue
    UNKNOWN,
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
    NX,
    SWITCH;

    private static final DeviceOS[] VALUES = values();

    public static DeviceOS getById(int id) {
        return id < VALUES.length ? VALUES[id] : VALUES[0];
    }
}
