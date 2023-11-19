package org.geysermc.floodgate.fabric.util;

import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.floodgate.mod.util.ModPlatformUtils;

public class FabricPlatformUtils extends ModPlatformUtils {

    @Override
    public AuthType authType() {
        if (server.usesAuthentication()) {
            return AuthType.ONLINE;
        }
        return isProxied() ? AuthType.PROXIED : AuthType.OFFLINE;
    }

    private boolean isProxied() {
        return FabricLoader.getInstance().isModLoaded("fabricproxy-lite") ||
        FabricLoader.getInstance().isModLoaded("fabricproxy") ||
        FabricLoader.getInstance().isModLoaded("fabroxy") ||
        FabricLoader.getInstance().isModLoaded("fabricproxy-legacy");
    }
}
