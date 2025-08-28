package org.geysermc.floodgate.mod.util;

import io.micronaut.context.BeanProvider;
import jakarta.inject.Inject;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;

public class ModPlatformUtils extends PlatformUtils {

    @Inject
    BeanProvider<MinecraftServer> minecraftServer;

    @Override
    public AuthType authType() {
        // TODO proxied auth type
        return minecraftServer.get().usesAuthentication() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    @Override
    public String serverImplementationName() {
        return minecraftServer.get().getServerModName();
    }
}
