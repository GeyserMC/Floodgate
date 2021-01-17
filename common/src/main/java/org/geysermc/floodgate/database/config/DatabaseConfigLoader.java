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

package org.geysermc.floodgate.database.config;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class DatabaseConfigLoader {
    private Yaml yaml;

    @Inject
    @Named("dataDirectory")
    private Path dataDirectory;

    @Inject
    @Named("databaseName")
    private String name;

    @Inject
    @Named("databaseClassLoader")
    private ClassLoader classLoader;

    @Inject
    @Named("databaseInitData")
    private JsonObject initData;

    @Inject
    public void init() {
        yaml = new Yaml(new CustomClassLoaderConstructor(classLoader));
        yaml.setBeanAccess(BeanAccess.FIELD);
    }

    /**
     * This will load the config if it already exists or will create the config from the default
     * config file if it doesn't exist.
     *
     * @param configType the class to parse the config into
     * @param <T>        type that extends the base DatabaseConfig class
     * @return the config if successful or null if not. It'll return null if the database didn't
     * provide a config or if there is no config present nor default config available or if an error
     * occurred while executing this method.
     */
    public <T extends DatabaseConfig> T loadAs(Class<T> configType) {
        if (!initData.has("config")) {
            return null;
        }

        String configFile = initData.get("config").getAsString();
        Path configPath = dataDirectory.resolve(name).resolve(configFile);

        // return the existing config
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                return yaml.loadAs(reader, configType);
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        // make directories
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }

        // load default config resource
        try (InputStream configStream = classLoader.getResourceAsStream(configFile)) {
            if (configStream == null) {
                return null;
            }

            // copy resource and load config
            if (!configStream.markSupported()) {
                Files.copy(configStream, configPath);
                try (InputStream configStream1 = classLoader.getResourceAsStream(configFile)) {
                    return yaml.loadAs(configStream1, configType);
                }
            }

            configStream.mark(Integer.MAX_VALUE);
            Files.copy(configStream, configPath);
            configStream.reset();
            return yaml.loadAs(configStream, configType);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
