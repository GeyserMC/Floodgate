package org.geysermc.floodgate.mod.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class ModMixinConfigPluginImpl {

    public static boolean applyProxyFix() {
        return FabricLoader.getInstance().isModLoaded("fabricproxy-lite");
    }
}
