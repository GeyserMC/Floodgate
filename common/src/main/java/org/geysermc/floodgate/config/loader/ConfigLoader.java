/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.config.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.config.updater.ConfigUpdater;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.crypto.KeyProducer;

@RequiredArgsConstructor
public final class ConfigLoader {
    private final Path dataFolder;
    private final Class<? extends FloodgateConfig> configClass;
    private final DefaultConfigHandler configCreator;
    private final ConfigUpdater updater;

    private final KeyProducer keyProducer;
    private final FloodgateCipher cipher;

    private final FloodgateLogger logger;

    @SuppressWarnings("unchecked")
    public <T extends FloodgateConfig> T load() {
        Path configPath = dataFolder.resolve("config.yml");

        String defaultConfigName = "config.yml";
        boolean proxy = ProxyFloodgateConfig.class.isAssignableFrom(configClass);
        if (proxy) {
            defaultConfigName = "proxy-" + defaultConfigName;
        }

        boolean newConfig = !Files.exists(configPath);
        if (newConfig) {
            try {
                configCreator.createDefaultConfig(defaultConfigName, configPath);
            } catch (Exception exception) {
                logger.error("Error while creating config", exception);
            }
        }

        T configInstance;
        try {
            // check and update if the config is outdated
            if (!newConfig) {
                updater.update(defaultConfigName);
            }

            FloodgateConfig config = ConfigInitializer.initializeFrom(
                    Files.newInputStream(configPath), configClass);

            try {
                configInstance = (T) config;
            } catch (ClassCastException exception) {
                logger.error("Failed to cast config file to required class.", exception);
                throw new RuntimeException(exception);
            }
        } catch (Exception exception) {
            logger.error("Error while loading config", exception);
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file", exception);
        }

        Path keyPath = dataFolder.resolve(configInstance.getKeyFileName());
        // don't assume that the key always exists with the existence of a config
        if (!Files.exists(keyPath)) {
            try {
                Key key = keyProducer.produce();
                cipher.init(key);

                String test = "abcdefghijklmnopqrstuvwxyz0123456789";
                byte[] encrypted = cipher.encryptFromString(test);
                String decrypted = cipher.decryptToString(encrypted);

                if (!test.equals(decrypted)) {
                    logger.error("Whoops, we tested the generated Floodgate keys but " +
                            "the decrypted test message doesn't match the original.\n" +
                            "Original message: " + test + "." +
                            "Decrypted message: " + decrypted + ".\n" +
                            "The encrypted message itself: " + new String(encrypted)
                    );
                    throw new RuntimeException(
                            "Tested the generated public and private key but, " +
                                    "the decrypted message doesn't match the original!"
                    );
                }

                Files.write(keyPath, key.getEncoded());
            } catch (Exception exception) {
                logger.error("Error while creating key", exception);
            }
        }

        try {
            Key key = keyProducer.produceFrom(keyPath);
            cipher.init(key);
            configInstance.setKey(key);
        } catch (IOException exception) {
            logger.error("Error while reading the key", exception);
            throw new RuntimeException("Failed to read the key!", exception);
        }

        return configInstance;
    }
}
