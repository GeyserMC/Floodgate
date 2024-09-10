package org.geysermc.floodgate.mod.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class MixinConfigPluginImpl {

    public static boolean isGeyserLoaded() {
        return FabricLoader.getInstance().isModLoaded("geyser-fabric");
    }

    public static boolean applyProxyFix() {
        return FabricLoader.getInstance().isModLoaded("fabricproxy-lite");
    }
}
