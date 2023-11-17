package org.geysermc.floodgate.mod.util;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;

public class ModPlatformUtils extends PlatformUtils {

    @Inject
    @Named("minecraftServer")
    private MinecraftServer server;

    @Override
    public AuthType authType() {
        return server.usesAuthentication() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public String serverImplementationName() {
        return server.getServerModName();
    }
}
