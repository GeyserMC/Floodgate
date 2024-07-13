/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import lombok.Getter;
import org.geysermc.configutils.loader.callback.CallbackResult;
import org.geysermc.configutils.loader.callback.GenericPostInitializeCallback;

/**
 * The global Floodgate configuration file used in every platform. Some platforms have their own
 * addition to the global configuration like {@link ProxyFloodgateConfig} for the proxies.
 */
@Getter
public class FloodgateConfig implements GenericPostInitializeCallback<ConfigLoader> {
    private String keyFileName;
    private String usernamePrefix = "";
    private boolean replaceSpaces;

    private String defaultLocale;

    private DisconnectMessages disconnect;
    private PlayerLinkConfig playerLink;
    private MetricsConfig metrics;

    private boolean debug;
    private int configVersion;


    private Key key;
    private String rawUsernamePrefix;

    public boolean isProxy() {
        return this instanceof ProxyFloodgateConfig;
    }

    @Override
    public CallbackResult postInitialize(ConfigLoader loader) {
        Path keyPath = loader.getDataDirectory().resolve(getKeyFileName());

        // don't assume that the key always exists with the existence of a config
        if (!Files.exists(keyPath)) {
            loader.generateKey(keyPath);
        }

        try {
            Key floodgateKey = loader.getKeyProducer().produceFrom(keyPath);
            loader.getCipher().init(floodgateKey);
            key = floodgateKey;
        } catch (IOException exception) {
            return CallbackResult.failed(exception.getMessage());
        }

        rawUsernamePrefix = usernamePrefix;

        // Java usernames can't be longer than 16 chars
        if (usernamePrefix.length() >= 16) {
            usernamePrefix = ".";
        }

        return CallbackResult.ok();
    }

    @Getter
    public static class DisconnectMessages {
        private String invalidKey;
        private String invalidArgumentsLength;
    }

    @Getter
    public static class PlayerLinkConfig {
        private boolean enabled;
        private boolean requireLink;
        private boolean enableOwnLinking;
        private boolean allowed;
        private long linkCodeTimeout;
        private String type;
        private boolean enableGlobalLinking;
    }

    @Getter
    public static class MetricsConfig {
        private boolean enabled;
        private String uuid;
    }
}
