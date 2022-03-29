package com.minekube.connect.util;

import com.minekube.connect.platform.util.PlatformUtils;
import java.util.List;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.ProtocolConstants;

public final class BungeePlatformUtils extends PlatformUtils {
    private final ProxyServer proxyServer = ProxyServer.getInstance();

    @Override
    public AuthType authType() {
        return proxyServer.getConfig().isOnlineMode() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        List<String> versions = ProtocolConstants.SUPPORTED_VERSIONS;
        return versions.get(versions.size() - 1);
    }
}