package com.minekube.connect.util;

import com.google.inject.Inject;
import com.minekube.connect.platform.util.PlatformUtils;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ProxyServer;

public final class VelocityPlatformUtils extends PlatformUtils {
    @Inject
    private ProxyServer server;

    @Override
    public AuthType authType() {
        return server.getConfiguration().isOnlineMode() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return ProtocolVersion.MAXIMUM_VERSION.getMostRecentSupportedVersion();
    }
}