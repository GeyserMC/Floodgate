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
public interface FloodgateConfig extends GenericPostInitializeCallback<Path> {
    default boolean proxy() {
        return this instanceof ProxyFloodgateConfig;
    }

    @Override
    default CallbackResult postInitialize(Path dataDirectory) {
        Path keyPath = dataDirectory.resolve(keyFileName());

        // don't assume that the key always exists with the existence of a config
        if (!Files.exists(keyPath)) {
            // TODO improve message and also link to article about corrupted keys/WinSCP/FTP
            // Like, where is key.pem?
            // TODO don't be so noisy with error. It makes it hard to understand what the error is.
            return CallbackResult.failed("Floodgate requires a key file! " +
                    "Copy your key file from Geyser (key.pem) and paste it into " + keyPath);
        }

        //todo remove key file name config option

        rawUsernamePrefix(usernamePrefix());

        // Java usernames can't be longer than 16 chars
        if (usernamePrefix().length() >= 16) {
            usernamePrefix(".");
        }

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

    DatabaseConfig database();

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
        @Comment
        @DefaultString("Please connect through the official Geyser")
        String invalidKey();

        @Comment
        @DefaultString("Expected {} arguments, got {}. Is Geyser up-to-date?")
        String invalidArgumentsLength();
    }

    @ConfigSection
    interface DatabaseConfig {
        @Comment
        @DefaultBoolean(true)
        boolean enabled();

        @Comment
        @DefaultString("h2")
        String type();
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
