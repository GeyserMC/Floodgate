package com.minekube.connect.util;

import com.minekube.connect.platform.util.PlatformUtils;
import org.bukkit.Bukkit;

public class SpigotPlatformUtils extends PlatformUtils {
    @Override
    public AuthType authType() {
        if (Bukkit.getOnlineMode()) {
            return AuthType.ONLINE;
        }
        return ProxyUtils.isProxyData() ? AuthType.PROXIED : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];
    }

    @Override
    public String serverImplementationName() {
        return Bukkit.getServer().getName();
    }
}
