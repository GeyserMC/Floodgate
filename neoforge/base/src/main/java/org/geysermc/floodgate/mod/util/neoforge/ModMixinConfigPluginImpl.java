package org.geysermc.floodgate.mod.util.neoforge;

import net.neoforged.fml.loading.LoadingModList;

public class ModMixinConfigPluginImpl {
    public static boolean isGeyserLoaded() {
        return LoadingModList.get().getModFileById("geyser_neoforge") != null;
    }

    public static boolean applyProxyFix() {
        return false;
    }
}
