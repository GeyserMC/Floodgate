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

import com.google.common.base.Ascii;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.loader.DefaultConfigHandler;

public final class ConfigFileUpdater {
    @Inject private FloodgateLogger logger;
    @Inject private DefaultConfigHandler defaultConfigHandler;

    /**
     * Simple config file updater. Please note that all the keys should be unique and that this
     * system wasn't made for complex configurations.
     *
     * @param configLocation        the location of the Floodgate config
     * @param currentVersion        the key value map of the current config
     * @param renames               name changes introduced in this version. new (key) to old
     *                              (value)
     * @param defaultConfigLocation the location of the default Floodgate config
     * @throws IOException if an I/O error occurs
     */
    public void update(Path configLocation, Map<String, Object> currentVersion,
                       Map<String, String> renames, String defaultConfigLocation)
            throws IOException {
        List<String> notFound = new ArrayList<>();
        List<String> newConfig = defaultConfigHandler.loadDefaultConfig(defaultConfigLocation);

        String spaces = "";
        Map<String, Object> map = null;

        String line;
        for (int i = 0; i < newConfig.size(); i++) {
            line = newConfig.get(i);
            // we don't have to check comments or empty lines
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

            StringBuilder currentSpaces = new StringBuilder();
            while (line.charAt(currentSpaces.length()) == Ascii.SPACE) {
                currentSpaces.append(Ascii.SPACE);
            }

            // end of subcategory
            if (spaces.length() > 0 && currentSpaces.length() < spaces.length()) {
                // we can assume this since we don't allow subcategories of subcategories
                spaces = "";
                map = null;
            }

            int splitIndex = line.indexOf(':');
            // if the line has a 'key: value' structure
            if (splitIndex != -1) {

                // start of a subcategory
                if (line.length() == splitIndex + 1) {
                    if (currentSpaces.length() > 0) {
                        throw new IllegalStateException(
                                "Config too complex! I can't understand subcategories of a subcategory");
                    }

                    spaces = "  ";
                    //todo allow rename of subcategory?
                    map = (Map<String, Object>) currentVersion.get(line.substring(0, splitIndex));
                    map.entrySet().forEach(System.out::println);
                    continue;
                }

                String name = line.substring(spaces.length(), splitIndex);
                String oldName = renames.getOrDefault(name, name);

                Object value;
                if (map != null) {
                    value = map.get(oldName);
                } else {
                    value = currentVersion.get(spaces + oldName);
                }

                // use default value if the key doesn't exist in the current version
                if (value == null) {
                    notFound.add(name);
                    continue;
                }

                if (value instanceof String) {
                    String v = (String) value;
                    if (!v.startsWith("\"") || !v.endsWith("\"")) {
                        value = "\"" + value + "\"";
                    }
                    //todo this doesn't update {0} {1} to {} {} e.g.
                }

                logger.debug(name + " has been changed to " + value);
                newConfig.set(i, spaces + name + ": " + value);
            }
        }

        Files.deleteIfExists(configLocation.getParent().resolve("config-old.yml"));
        Files.copy(configLocation, configLocation.getParent().resolve("config-old.yml"));
        Files.write(configLocation, newConfig);

        logger.info("Successfully updated the config file! " +
                "Your old config has been moved to config-old.yml");

        if (notFound.size() > 0) {
            StringBuilder messageBuilder = new StringBuilder(
                    "Please note that the following keys we not found in the old config and " +
                            "are now using the default Floodgate config value. Missing/new keys: ");

            boolean first = true;
            for (String value : notFound) {
                if (!first) {
                    messageBuilder.append(", ");
                }

                String renamed = renames.get(value);
                if (renamed != null) {
                    messageBuilder.append(renamed).append(" to ");
                }

                messageBuilder.append(value);

                first = false;
            }

            logger.info(messageBuilder.toString());
        }
    }
}
