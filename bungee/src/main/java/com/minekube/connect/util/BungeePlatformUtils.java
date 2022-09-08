package com.minekube.connect.util;

import com.minekube.connect.platform.util.PlatformUtils;
import java.lang.reflect.Field;
import java.util.List;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.ProtocolConstants;

@SuppressWarnings("ConstantConditions")
public final class BungeePlatformUtils extends PlatformUtils {
    private static final String LATEST_SUPPORTED_VERSION;
    private final ProxyServer proxyServer = ProxyServer.getInstance();

    static {
        int protocolNumber = -1;
        String versionName = "";

        for (Field field : ProtocolConstants.class.getFields()) {
            if (!field.getName().startsWith("MINECRAFT_")) {
                continue;
            }

            int fieldValue = ReflectionUtils.castedStaticValue(field);
            if (fieldValue > protocolNumber) {
                protocolNumber = fieldValue;
                versionName = field.getName().substring(10).replace('_', '.');
            }
        }

        if (protocolNumber == -1) {
            List<String> versions = ProtocolConstants.SUPPORTED_VERSIONS;
            versionName = versions.get(versions.size() - 1);
        }
        LATEST_SUPPORTED_VERSION = versionName;
    }

    @Override
    public AuthType authType() {
        return proxyServer.getConfig().isOnlineMode() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return LATEST_SUPPORTED_VERSION;
    }

    @Override
    public String serverImplementationName() {
        return proxyServer.getName();
    }
}