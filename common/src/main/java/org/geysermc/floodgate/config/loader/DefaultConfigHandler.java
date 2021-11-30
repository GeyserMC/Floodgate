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

import static org.geysermc.floodgate.util.MessageFormatter.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.geysermc.floodgate.util.Utils;

public class DefaultConfigHandler {
    public void createDefaultConfig(String defaultConfigLocation, Path configPath) throws IOException {
        List<String> configLines = loadDefaultConfig(defaultConfigLocation);

        // writing the new config file
        Files.write(configPath, configLines);
    }

    public List<String> loadDefaultConfig(String defaultConfigLocation)
            throws IOException {
        List<String> lines = Utils.readAllLines(defaultConfigLocation);

        List<String> configLines = new ArrayList<>();
        String parentConfig = null;
        List<String> parentLines = null;

        int lastInsertLine = -1;
        int tempAddAfter = -1;

        for (String line : lines) {
            // >>(space) or >>|
            if (line.startsWith(">>")) {
                if (line.length() >= 3) {

                    // define parent file
                    if (line.charAt(2) == ' ') {
                        if (tempAddAfter != -1) {
                            throw new IllegalStateException(
                                    "Cannot define new parent without closing the current section");
                        }
                        parentConfig = line.substring(3);
                        parentLines = null;
                        lastInsertLine = -1;
                        continue;
                    }

                    // define start / end of insert section
                    if (line.charAt(2) == '|') {
                        // end section
                        if (line.length() == 3) {
                            if (tempAddAfter == -1) {
                                throw new IllegalStateException("Cannot close an unclosed section");
                            }
                            lastInsertLine = tempAddAfter;
                            tempAddAfter = -1;
                            continue;
                        }

                        // start insert section
                        if (parentConfig == null) {
                            throw new IllegalStateException(
                                    "Cannot start insert section without providing a parent");
                        }

                        if (tempAddAfter != -1) {
                            throw new IllegalStateException(
                                    "Cannot start section with an unclosed section");
                        }

                        // note that addAfter starts counting from 1
                        int addAfter = Integer.parseInt(line.substring(4)) - 1;
                        if (lastInsertLine > -1 && addAfter < lastInsertLine) {
                            throw new IllegalStateException(format(
                                    "Cannot add the same lines twice {} {}",
                                    addAfter, lastInsertLine
                            ));
                        }

                        // as you can see by this implementation
                        // we don't support parent files in parent files

                        if (lastInsertLine == -1) {
                            parentLines = Utils.readAllLines(parentConfig);

                            for (int i = 0; i <= addAfter; i++) {
                                configLines.add(parentLines.get(i));
                            }
                        } else {
                            for (int i = lastInsertLine; i <= addAfter; i++) {
                                configLines.add(parentLines.get(i));
                            }
                        }

                        tempAddAfter = addAfter;
                        continue;
                    }

                    if (line.charAt(2) == '*') {
                        if (parentConfig == null) {
                            throw new IllegalStateException(
                                    "Cannot write rest of the parent without providing a parent");
                        }

                        if (tempAddAfter != -1) {
                            throw new IllegalStateException(
                                    "Cannot write rest of the parent config while an insert section is still open");
                        }

                        if (lastInsertLine == -1) {
                            parentLines = Utils.readAllLines(parentConfig);
                            configLines.addAll(parentLines);
                            continue;
                        }

                        // the lastInsertLine has already been printed, so we won't print it twice
                        for (int i = lastInsertLine + 1; i < parentLines.size(); i++) {
                            configLines.add(parentLines.get(i));
                        }
                        continue;
                    }

                    throw new IllegalStateException(
                            "The use of >>" + line.charAt(2) + " is unknown");
                }
                throw new IllegalStateException("Unable do something with just >>");
            }
            // everything else: comments and key/value lines will be added
            configLines.add(line);
        }

        return configLines;
    }
}
