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

package org.geysermc.floodgate.config.updater;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geysermc.floodgate.util.MessageFormatter.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.loader.ConfigLoader;
import org.yaml.snakeyaml.Yaml;

@RequiredArgsConstructor
public final class ConfigUpdater {
    private static final int CONFIG_VERSION = 2;
    private final Path dataFolder;
    private final ConfigFileUpdater fileUpdater;
    private final FloodgateLogger logger;

    public void update(ConfigLoader loader, String defaultConfigLocation) {
        Path configLocation = dataFolder.resolve("config.yml");

        Map<String, Object> config;

        try (BufferedReader configReader = Files.newBufferedReader(configLocation)) {
            config = new Yaml().load(configReader);
        } catch (IOException exception) {
            logger.error("Error while opening the config file", exception);
            throw new RuntimeException("Failed to update config", exception);
        }

        // new name -> old name
        Map<String, String> renames = new HashMap<>();

        int version = 0; // pre-rewrite is the default config version

        Object versionElement = config.get("config-version");
        // only rewrite configs have a config-version
        if (versionElement == null) {
            logger.warn("We've detected a pre-rewrite config file, please note that Floodgate " +
                    "doesn't not work properly if you don't update your Floodgate key used on " +
                    "all your servers (including Geyser). We'll try to update your Floodgate " +
                    "config now and we'll also generate a new Floodgate key for you, but if " +
                    "you're running a network or if you're running a Spigot server with " +
                    "Geyser Standalone please update as you'll no longer be able to connect.");
            renames.put("enabled", "enable"); //todo make dump system and add a boolean 'found-legacy-key' or something like that
            renames.put("allowed", "allow-linking");

            // relocate the old key so that they can restore it if it was a new key
            Path keyFilePath = dataFolder.resolve((String) config.get("key-file-name"));
            if (Files.exists(keyFilePath)) {
                try {
                    Files.copy(keyFilePath, dataFolder.resolve("old-key.pem"));
                } catch (IOException exception) {
                    throw new RuntimeException(
                            "Failed to relocate the old key to make place for a new key",
                            exception);
                }
            }
            loader.generateKey(keyFilePath);
        } else {
            // get (and verify) the config version
            checkArgument(
                    versionElement instanceof Integer,
                    "Config version should be an integer. Did someone mess with the config?"
            );

            version = (int) versionElement;
            checkArgument(
                    version > 0 && version <= CONFIG_VERSION,
                    format("Config is newer then possible on this version! Expected {}, got {}",
                            CONFIG_VERSION, version));
        }

        // config is already up-to-date
        if (version == CONFIG_VERSION) {
            return;
        }

        if (version < 2) {
            // renamed 'use-global-linking' to 'enable-global-linking'
            // and added 'enable-own-linking'
            renames.put("enable-global-linking", "use-global-linking");
        }

        try {
            fileUpdater.update(configLocation, config, renames, defaultConfigLocation);
        } catch (IOException exception) {
            logger.error("Error while updating the config file", exception);
            throw new RuntimeException("Failed to update config", exception);
        }
    }
}
