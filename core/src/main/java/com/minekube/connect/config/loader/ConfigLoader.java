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

package com.minekube.connect.config.loader;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.config.ProxyConnectConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ConfigLoader {
    private final Path dataFolder;
    private final Class<? extends ConnectConfig> configClass;
    private final DefaultConfigHandler configCreator;

    private final ConnectLogger logger;

    @SuppressWarnings("unchecked")
    public <T extends ConnectConfig> T load() {
        Path configPath = dataFolder.resolve("config.yml");

        String defaultConfigName = "config.yml";
        boolean proxy = ProxyConnectConfig.class.isAssignableFrom(configClass);
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
            ConnectConfig config = ConfigInitializer.initializeFrom(
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

        return configInstance;
    }

}
