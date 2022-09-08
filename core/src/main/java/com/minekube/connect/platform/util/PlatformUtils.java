package com.minekube.connect.platform.util;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PlatformUtils {
    /**
     * Returns the authentication type used on the platform
     *
     * @return the authentication type
     */
    public abstract AuthType authType();

    /**
     * Returns the Minecraft version the server is based on (or the most recent supported version
     * for proxy platforms)
     *
     * @return the Minecraft version
     */
    public abstract String minecraftVersion();


    /**
     * Returns the platform name
     *
     * @return the platform name
     */
    public abstract String serverImplementationName();

    public enum AuthType {
        ONLINE,
        PROXIED,
        OFFLINE
    }
}