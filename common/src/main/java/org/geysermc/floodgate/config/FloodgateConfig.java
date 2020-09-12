/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.security.Key;

/**
 * The global Floodgate configuration file used in every platform.
 * Some platforms have their own addition to the global configuration like
 * {@link ProxyFloodgateConfig} for the proxies.
 */
@Getter
public class FloodgateConfig {
    @JsonProperty(value = "key-file-name")
    private String keyFileName;
    @JsonProperty(value = "username-prefix")
    private String usernamePrefix;
    @JsonProperty(value = "replace-spaces")
    private boolean replaceSpaces;

    @JsonProperty(value = "disconnect")
    private DisconnectMessages messages;

    @JsonProperty(value = "player-link")
    private PlayerLinkConfig playerLink;

    @JsonProperty
    private boolean debug;

    @JsonProperty("config-version")
    private boolean configVersion;

    @JsonIgnore
    private Key key = null;

    @Getter
    public static class DisconnectMessages {
        @JsonProperty("invalid-key")
        private String invalidKey;
        @JsonProperty("invalid-arguments-length")
        private String invalidArgumentsLength;
    }

    @Getter
    public static class PlayerLinkConfig {
        @JsonProperty("enable")
        private boolean enabled;
        @JsonProperty("allow-linking")
        private boolean allowLinking;
        @JsonProperty("link-code-timeout")
        private long linkCodeTimeout;
        @JsonProperty("type")
        private String type;
        @JsonProperty("auto-download")
        private boolean autoDownload;
    }

    public void setKey(Key key) {
        if (this.key == null) {
            this.key = key;
        }
    }

    public boolean isProxy() {
        return this instanceof ProxyFloodgateConfig;
    }
}
