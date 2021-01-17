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

package org.geysermc.floodgate.link;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.Utils;

@Singleton
@SuppressWarnings("unchecked")
public final class PlayerLinkLoader {
    @Inject private Injector injector;
    @Inject private FloodgateConfig config;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("dataDirectory")
    private Path dataDirectory;

    public PlayerLink load() {
        if (config == null) {
            throw new IllegalStateException("Config cannot be null!");
        }

        FloodgateConfig.PlayerLinkConfig linkingConfig = config.getPlayerLink();
        if (!linkingConfig.isEnabled()) {
            return new DisabledPlayerLink();
        }

        // we use our own internal PlayerLinking when global linking is enabled
        if (linkingConfig.isUseGlobalLinking()) {
            return injector.getInstance(GlobalPlayerLinking.class);
        }

        List<Path> files;
        try {
            files = Files.list(dataDirectory)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith("jar"))
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            logger.error("Failed to list possible database implementations", exception);
            return null;
        }

        if (files.isEmpty()) {
            logger.error("Failed to find a database implementation");
            return null;
        }

        Path implementationPath = files.get(0);
        final String databaseName;

        // We only want to load one database implementation
        if (files.size() > 1) {
            boolean found = false;
            databaseName = linkingConfig.getType();

            String expectedName = "floodgate-" + databaseName + "-database.jar";
            for (Path path : files) {
                if (expectedName.equalsIgnoreCase(path.getFileName().toString())) {
                    implementationPath = path;
                    found = true;
                }
            }

            if (!found) {
                logger.error("Failed to find an implementation for type: {}",
                        linkingConfig.getType());
                return null;
            }
        } else {
            String name = implementationPath.getFileName().toString();
            if (!Utils.isValidDatabaseName(name)) {
                logger.error("Found database {} but the name doesn't match {}",
                        name, Constants.DATABASE_NAME_FORMAT);
                return null;
            }
            int firstSplit = name.indexOf('-') + 1;
            databaseName = name.substring(firstSplit, name.indexOf('-', firstSplit));
        }

        boolean init = true;

        try {
            URL pluginUrl = implementationPath.toUri().toURL();

            try (URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{pluginUrl}, PlayerLinkLoader.class.getClassLoader())) {

                String mainClassName;
                JsonObject linkConfig;

                try (InputStream linkConfigStream =
                             classLoader.getResourceAsStream("init.json")) {

                    requireNonNull(linkConfigStream, "Implementation should have an init file");

                    linkConfig = new Gson().fromJson(
                            new InputStreamReader(linkConfigStream), JsonObject.class
                    );

                    mainClassName = linkConfig.get("mainClass").getAsString();
                }

                Class<? extends PlayerLink> mainClass =
                        (Class<? extends PlayerLink>) classLoader.loadClass(mainClassName);

                init = false;

                Injector linkInjector = injector.createChildInjector(binder -> {
                    binder.bind(String.class)
                            .annotatedWith(Names.named("databaseName"))
                            .toInstance(databaseName);
                    binder.bind(ClassLoader.class).annotatedWith(
                            Names.named("databaseClassLoader")).toInstance(classLoader);
                    binder.bind(JsonObject.class)
                            .annotatedWith(Names.named("databaseInitData"))
                            .toInstance(linkConfig);
                });

                PlayerLink instance = linkInjector.getInstance(mainClass);
                instance.load();
                return instance;
            }
        } catch (ClassCastException exception) {
            logger.error("The database implementation ({}) doesn't extend the PlayerLink class!",
                    implementationPath.getFileName().toString(), exception);
            return null;
        } catch (Exception exception) {
            if (init) {
                logger.error("Error while initialising database jar", exception);
            } else {
                logger.error("Error while loading database jar", exception);
            }
            return null;
        }
    }
}
