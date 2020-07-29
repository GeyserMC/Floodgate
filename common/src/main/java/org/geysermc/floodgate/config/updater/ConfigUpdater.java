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

import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@RequiredArgsConstructor
public class ConfigUpdater {
    private final Path dataFolder;
    private final ConfigFileUpdater fileUpdater;
    private final FloodgateLogger logger;

    public void update(Path defaultConfigLocation) {
        Path configLocation = dataFolder.resolve("config.yml");

        BufferedReader configReader;
        try {
            configReader = Files.newBufferedReader(configLocation);
        } catch (IOException exception) {
            logger.error("Error while opening the config file", exception);
            throw new RuntimeException("Failed to update config");
        }

        Map<String, Object> config = new Yaml().load(configReader);
        // currently unused, but can be used when a config name has been changed
        Map<String, String> renames = new HashMap<>(0);

        Object versionElement = config.get("config-version");
        // not a pre-rewrite config
        if (versionElement != null) {
            checkArgument(
                    versionElement instanceof Integer,
                    "Config version should be an integer. Did someone mess with the config?"
            );

            int version = (int) versionElement;
            checkArgument(
                    version == 1,
                    "Config is newer then possible on this version! Expected 1, got " + version
            );

            // config is already up-to-date
            if (version == 1) {
                return;
            }
        }

        try {
            fileUpdater.update(configLocation, config, renames, defaultConfigLocation);
        } catch (IOException exception) {
            logger.error("Error while updating the config file", exception);
            throw new RuntimeException("Failed to update config");
        }
    }
}
