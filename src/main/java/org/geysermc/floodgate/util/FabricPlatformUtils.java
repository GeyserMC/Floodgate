package org.geysermc.floodgate.util;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import org.geysermc.floodgate.platform.util.PlatformUtils;

public class FabricPlatformUtils extends PlatformUtils {
    private static MinecraftServer SERVER;

    @Override
    public AuthType authType() {
        return SERVER.usesAuthentication() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public String serverImplementationName() {
        return "Fabric";
    }

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }
}
