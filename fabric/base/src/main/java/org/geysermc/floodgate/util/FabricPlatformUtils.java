package org.geysermc.floodgate.util;

import net.minecraft.SharedConstants;
import org.geysermc.floodgate.MinecraftServerHolder;
import org.geysermc.floodgate.platform.util.PlatformUtils;

public class FabricPlatformUtils extends PlatformUtils {
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
