/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.config.updater.ConfigUpdater;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.crypto.KeyProducer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;

@RequiredArgsConstructor
public class ConfigLoader {
    private final Path dataFolder;
    private final Class<? extends FloodgateConfig> configClass;
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

        Path defaultConfigPath;
        try {
            defaultConfigPath = Paths.get("./" + defaultConfigName);
        } catch (RuntimeException exception) {
            logger.error("Failed to get the default config location", exception);
            throw new RuntimeException("Failed to get the default config location");
        }

        boolean newConfig = !Files.exists(configPath);
        try {
            if (newConfig) {
                InputStream newConfigFile =
                        ConfigLoader.class.getClassLoader().getResourceAsStream(defaultConfigName);
                if (newConfigFile == null) {
                    throw new RuntimeException("Failed to get the default config file!");
                }

                Files.copy(newConfigFile, configPath);

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

                Files.write(dataFolder.resolve("key.pem"), key.getEncoded());
            }
        } catch (Exception exception) {
            logger.error("Error while creating config", exception);
        }

        T configInstance;
        try {
            // check and update if the config is outdated
            if (!newConfig) {
                updater.update(defaultConfigPath);
            }

            configInstance = (T) new ObjectMapper(new YAMLFactory())
                    .readValue(Files.readAllBytes(configPath), configClass);
        } catch (ClassCastException exception) {
            logger.error("Provided class {} cannot be cast to the required return type",
                    configClass.getName());

            throw new RuntimeException("Failed to load cast the config! " +
                    "Try to contact the platform developer");
        } catch (Exception exception) {
            logger.error("Error while loading config", exception);
            throw new RuntimeException("Failed to load the config! Try to delete the config file");
        }

        try {
            Key key = keyProducer.produceFrom(dataFolder.resolve(configInstance.getKeyFileName()));
            cipher.init(key);
            configInstance.setKey(key);
        } catch (IOException exception) {
            logger.error("Error while reading the key", exception);
            throw new RuntimeException("Failed to read the key!");
        }

        return configInstance;
    }
}
