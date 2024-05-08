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

package org.geysermc.floodgate.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.configutils.ConfigUtilities;
import org.geysermc.configutils.file.codec.PathFileCodec;
import org.geysermc.configutils.file.template.TemplateReader;
import org.geysermc.configutils.updater.change.Changes;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.crypto.KeyProducer;

@Getter
@RequiredArgsConstructor
public final class ConfigLoader {
    private final Path dataDirectory;
    private final Class<? extends FloodgateConfig> configClass;

    private final KeyProducer keyProducer;
    private final FloodgateCipher cipher;
    private final TemplateReader reader;

    @SuppressWarnings("unchecked")
    public <T extends FloodgateConfig> T load() {
        String templateFile = "config.yml";
        if (ProxyFloodgateConfig.class.isAssignableFrom(configClass)) {
            templateFile = "proxy-" + templateFile;
        }

        //todo old Floodgate logged a message when version = 0 and it generated a new key.
        // Might be nice to allow you to run a function for a specific version.

        // it would also be nice to have sections in versionBuilder so that you don't have to
        // provide the path all the time

        ConfigUtilities utilities =
                ConfigUtilities.builder()
                        .fileCodec(PathFileCodec.of(dataDirectory))
                        .configFile("config.yml")
                        .templateReader(reader)
                        .template(templateFile)
                        .changes(Changes.builder()
                                .version(1, Changes.versionBuilder()
                                        .keyRenamed("player-link.enable", "player-link.enabled")
                                        .keyRenamed("player-link.allow-linking", "player-link.allowed"))
                                .version(2, Changes.versionBuilder()
                                        .keyRenamed("player-link.use-global-linking", "player-link.enable-global-linking"))
                                .build())
                        .definePlaceholder("metrics.uuid", UUID::randomUUID)
                        .postInitializeCallbackArgument(this)
                        .build();

        try {
            return (T) utilities.executeOn(configClass);
        } catch (Throwable throwable) {
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file if this error persists",
                    throwable
            );
        }
    }

    public void generateKey(Path keyPath) {
        try {
            Key key = keyProducer.produce();
            cipher.init(key);

            String test = "abcdefghijklmnopqrstuvwxyz0123456789";
            byte[] encrypted = cipher.encryptFromString(test);
            String decrypted = cipher.decryptToString(encrypted);

            if (!test.equals(decrypted)) {
                throw new RuntimeException("Failed to decrypt test message.\n" +
                        "Original message: " + test + "." +
                        "Decrypted message: " + decrypted + ".\n" +
                        "The encrypted message itself: " + new String(encrypted)
                );
            }

            Files.write(keyPath, key.getEncoded());
        } catch (Exception exception) {
            throw new RuntimeException("Error while creating key", exception);
        }
    }
}
