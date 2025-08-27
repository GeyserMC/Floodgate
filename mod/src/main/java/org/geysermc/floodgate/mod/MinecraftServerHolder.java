package org.geysermc.floodgate.mod;

import net.minecraft.server.MinecraftServer;

public final class MinecraftServerHolder {
    // Static because commands *need* to be initialized before the server is available
    // Otherwise it would be a class variable
    private static MinecraftServer INSTANCE;

    public static MinecraftServer get() {
        return INSTANCE;
    }

    static void set(MinecraftServer instance) {
        INSTANCE = instance;
    }

    private MinecraftServerHolder() {
    }
}
