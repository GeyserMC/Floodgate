/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package com.minekube.connect.config;

import com.minekube.connect.util.Utils;
import java.util.List;
import lombok.Getter;

/**
 * The global configuration file used in every platform. Some platforms have their own addition to
 * the global configuration like {@link ProxyConnectConfig} for the proxies.
 */
@Getter
public class ConnectConfig {
    private String defaultLocale;

    private MetricsConfig metrics;

    private boolean debug;
    private int configVersion;

    /**
     * The endpoint name of this instance that is registered when calling the watch service for
     * listening for sessions for this endpoint.
     */
    private final String endpoint = Utils.randomString(5); // default to random name

    /**
     * Whether cracked players should be allowed to join.
     * If not set, Connect will automatically detect if the server allows cracked players.
     */
    private Boolean allowOfflineModePlayers;

    /**
     * Super endpoints are authorized to control this endpoint via Connect API.
     * e.g. disconnect players from this endpoint, send messages to players, etc.
     */
    private List<String> superEndpoints;

    private static final String ENDPOINT_ENV = System.getenv("CONNECT_ENDPOINT");

    public String getEndpoint() {
        if (ENDPOINT_ENV != null && !ENDPOINT_ENV.isEmpty()) {
            return ENDPOINT_ENV;
        }
        return endpoint;
    }

    public boolean isProxy() {
        return this instanceof ProxyConnectConfig;
    }

    @Getter
    public static class MetricsConfig {
        /**
         * If metrics should be disabled.
         */
        private boolean disabled;
        /**
         * The unique id that should be consistent for a server/proxy.
         */
        private String uuid;
    }
}
