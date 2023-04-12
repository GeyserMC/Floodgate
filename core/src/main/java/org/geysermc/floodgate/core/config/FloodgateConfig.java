/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import org.geysermc.configutils.loader.callback.CallbackResult;
import org.geysermc.configutils.loader.callback.GenericPostInitializeCallback;
import org.geysermc.configutils.node.meta.Comment;
import org.geysermc.configutils.node.meta.ConfigSection;
import org.geysermc.configutils.node.meta.ConfigVersion;
import org.geysermc.configutils.node.meta.Defaults.DefaultBoolean;
import org.geysermc.configutils.node.meta.Defaults.DefaultNumeric;
import org.geysermc.configutils.node.meta.Defaults.DefaultString;
import org.geysermc.configutils.node.meta.Exclude;
import org.geysermc.configutils.node.meta.Hidden;
import org.geysermc.configutils.node.meta.Placeholder;

/**
 * The global Floodgate configuration file used in every platform. Some platforms have their own
 * addition to the global configuration like {@link ProxyFloodgateConfig} for the proxies.
 */
@ConfigVersion(3)
public interface FloodgateConfig extends GenericPostInitializeCallback<ConfigLoader> {
    default boolean proxy() {
        return this instanceof ProxyFloodgateConfig;
    }

    @Override
    default CallbackResult postInitialize(ConfigLoader loader) {
        Path keyPath = loader.getDataDirectory().resolve(keyFileName());

        // don't assume that the key always exists with the existence of a config
        if (!Files.exists(keyPath)) {
            loader.generateKey(keyPath);
        }

        try {
            Key floodgateKey = loader.getKeyProducer().produceFrom(keyPath);
            loader.getCipher().init(floodgateKey);
            key(floodgateKey);
        } catch (IOException exception) {
            return CallbackResult.failed(exception.getMessage());
        }

        rawUsernamePrefix(usernamePrefix());

        // Java usernames can't be longer than 16 chars
        if (usernamePrefix().length() >= 16) {
            usernamePrefix(".");
        }

        // this happens before the config is serialized,
        // so the messages in the config will be translated
        loader.getLanguageManager().loadConfiguredDefaultLocale(this);

        return CallbackResult.ok();
    }

    @Comment
    @DefaultString("key.pem")
    String keyFileName();

    @Comment
    @DefaultString(".")
    String usernamePrefix();

    void usernamePrefix(String usernamePrefix);

    @Comment
    @DefaultBoolean(true)
    boolean replaceSpaces();

    @Comment
    @DefaultString("system")
    String defaultLocale();

    DisconnectMessages disconnect();

    @Comment
    PlayerLinkConfig playerLink();

    MetricsConfig metrics();

    @Hidden
    @DefaultBoolean
    boolean debug();

    @Exclude Key key();

    void key(Key key);

    @Exclude String rawUsernamePrefix();

    void rawUsernamePrefix(String usernamePrefix);

    @ConfigSection
    interface DisconnectMessages {
        @DefaultString("Please connect through the official Geyser")
        String invalidKey();

        @DefaultString("Expected {} arguments, got {}. Is Geyser up-to-date?")
        String invalidArgumentsLength();
    }

    @ConfigSection
    interface PlayerLinkConfig {
        @Comment
        @DefaultBoolean(true)
        boolean enabled();

        @Comment
        @DefaultBoolean
        boolean requireLink();

        @Comment
        @DefaultBoolean
        boolean enableOwnLinking();

        @Comment
        @DefaultBoolean(true)
        boolean allowed();

        @Comment
        @DefaultNumeric(300)
        long linkCodeTimeout();

        @Comment
        @DefaultString("mysql")
        String type();

        @Comment
        @DefaultBoolean(true)
        boolean enableGlobalLinking();
    }

    @ConfigSection
    interface MetricsConfig {
        @DefaultBoolean(true)
        boolean enabled();

        @Placeholder
        String uuid();
    }
}
