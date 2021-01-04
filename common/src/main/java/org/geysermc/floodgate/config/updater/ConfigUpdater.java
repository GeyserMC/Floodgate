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
import org.yaml.snakeyaml.Yaml;

@RequiredArgsConstructor
public final class ConfigUpdater {
    private static final int CONFIG_VERSION = 1;
    private final Path dataFolder;
    private final ConfigFileUpdater fileUpdater;
    private final FloodgateLogger logger;

    public void update(String defaultConfigLocation) {
        Path configLocation = dataFolder.resolve("config.yml");

        BufferedReader configReader;
        try {
            configReader = Files.newBufferedReader(configLocation);
        } catch (IOException exception) {
            logger.error("Error while opening the config file", exception);
            throw new RuntimeException("Failed to update config");
        }

        Map<String, Object> config = new Yaml().load(configReader);

        // new name -> old name
        Map<String, String> renames = new HashMap<>();

        Object versionElement = config.get("config-version");
        // not a pre-rewrite config
        if (versionElement != null) {
            checkArgument(
                    versionElement instanceof Integer,
                    "Config version should be an integer. Did someone mess with the config?"
            );

            int version = (int) versionElement;
            checkArgument(
                    version == CONFIG_VERSION,
                    format("Config is newer then possible on this version! Expected {}, got {}",
                            CONFIG_VERSION, version));

            // config is already up-to-date
            if (version == CONFIG_VERSION) {
                return;
            }
        } else {
            logger.warn("You're using a pre-rewrite config file, please note that Floodgate will " +
                    "throw an exception if you didn't already update your Floodgate key" +
                    "(across all your servers, including Geyser). " +
                    "We'll still try to update the config," +
                    "but please regenerate the keys if it failed before asking for support.");
            renames.put("enabled", "enable"); //todo make dump system and add a boolean 'found-legacy-key' or something like that
            renames.put("allowed", "allow-linking");
        }

        try {
            fileUpdater.update(configLocation, config, renames, defaultConfigLocation);
        } catch (IOException exception) {
            logger.error("Error while updating the config file", exception);
            throw new RuntimeException("Failed to update config");
        }
    }
}
