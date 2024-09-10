package org.geysermc.floodgate.mod.util;

import net.minecraft.SharedConstants;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;
import org.geysermc.floodgate.mod.MinecraftServerHolder;

public class ModPlatformUtils extends PlatformUtils {
    @Override
    public AuthType authType() {
        return MinecraftServerHolder.get().usesAuthentication() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public String serverImplementationName() {
        return MinecraftServerHolder.get().getServerModName();
    }
}
