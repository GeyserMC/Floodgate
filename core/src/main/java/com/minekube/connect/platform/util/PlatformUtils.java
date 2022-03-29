package com.minekube.connect.platform.util;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PlatformUtils {
    /**
     * Returns the authentication type used on the platform
     */
    public abstract AuthType authType();

    /**
     * Returns the Minecraft version the server is based on (or the most recent supported version
     * for proxy platforms)
     */
    public abstract String minecraftVersion();

    public enum AuthType {
        ONLINE,
        PROXIED,
        OFFLINE
    }
}