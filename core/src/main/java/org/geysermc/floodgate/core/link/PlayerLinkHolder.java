/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.link;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Listener;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.config.FloodgateConfig.PlayerLinkConfig;
import org.geysermc.floodgate.core.event.lifecycle.ShutdownEvent;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.InjectorHolder;
import org.geysermc.floodgate.core.util.Utils;

@Listener
@Singleton
@SuppressWarnings("unchecked")
public final class PlayerLinkHolder {
    @Inject private Injector injector;
    @Inject private FloodgateConfig config;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("dataDirectory")
    private Path dataDirectory;

    private URLClassLoader classLoader;
    private PlayerLink instance;

    public @NonNull PlayerLink load() {
        if (instance != null) {
            return instance;
        }
        instance = load0();
        return instance;
    }

    private @NonNull PlayerLink load0() {
        if (config == null) {
            throw new IllegalStateException("Config cannot be null!");
        }

        PlayerLinkConfig linkConfig = config.getPlayerLink();
        if (!linkConfig.isEnabled()) {
            return new DisabledPlayerLink();
        }

        List<Path> files;
        try (Stream<Path> list = Files.list(dataDirectory)) {
            files = list
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            logger.error("Failed to list possible database implementations", exception);
            return new DisabledPlayerLink();
        }

        // we can skip the rest if global linking is enabled and no database implementations has
        // been found, or when global linking is enabled and own player linking is disabled.
        if (linkConfig.isEnableGlobalLinking() &&
                (files.isEmpty() || !linkConfig.isEnableOwnLinking())) {
            return injector.getInstance(GlobalPlayerLinking.class);
        }

        if (files.isEmpty()) {
            logger.error("Failed to find a database implementation");
            return new DisabledPlayerLink();
        }

        Path implementationPath = files.get(0);
        final String databaseName;

        // We only want to load one database implementation
        if (files.size() > 1) {
            boolean found = false;
            databaseName = linkConfig.getType();

            String expectedName = "floodgate-" + databaseName + "-database.jar";
            for (Path path : files) {
                if (expectedName.equalsIgnoreCase(path.getFileName().toString())) {
                    implementationPath = path;
                    found = true;
                }
            }

            if (!found) {
                logger.error(
                        "Failed to find an implementation for type: {}", linkConfig.getType()
                );
                return new DisabledPlayerLink();
            }
        } else {
            String name = implementationPath.getFileName().toString();
            if (!Utils.isValidDatabaseName(name)) {
                logger.error(
                        "Found database {} but the name doesn't match {}",
                        name, Constants.DATABASE_NAME_FORMAT
                );
                return new DisabledPlayerLink();
            }
            int firstSplit = name.indexOf('-') + 1;
            databaseName = name.substring(firstSplit, name.indexOf('-', firstSplit));
        }

        boolean init = true;

        try {
            URL pluginUrl = implementationPath.toUri().toURL();

            // we don't have a way to close this properly since we have no stop method and we have
            // to be able to load classes on the fly, but that doesn't matter anyway since Floodgate
            // doesn't support reloading
            classLoader = new URLClassLoader(
                    new URL[]{pluginUrl},
                    PlayerLinkHolder.class.getClassLoader()
            );

            String mainClassName;
            JsonObject dbInitConfig;

            try (InputStream linkConfigStream = classLoader.getResourceAsStream("init.json")) {
                requireNonNull(linkConfigStream, "Implementation should have an init file");

                dbInitConfig = new Gson().fromJson(
                        new InputStreamReader(linkConfigStream), JsonObject.class
                );

                mainClassName = dbInitConfig.get("mainClass").getAsString();
            }

            Class<? extends PlayerLink> mainClass =
                    (Class<? extends PlayerLink>) classLoader.loadClass(mainClassName);

            init = false;

            InjectorHolder injectorHolder = new InjectorHolder();
            Injector linkInjector = injector.createChildInjector(binder -> {
                binder.bind(String.class)
                        .annotatedWith(Names.named("databaseName"))
                        .toInstance(databaseName);
                binder.bind(ClassLoader.class).annotatedWith(
                        Names.named("databaseClassLoader")).toInstance(classLoader);
                binder.bind(JsonObject.class)
                        .annotatedWith(Names.named("databaseInitData"))
                        .toInstance(dbInitConfig);
                binder.bind(InjectorHolder.class)
                        .toInstance(injectorHolder);
            });
            injectorHolder.set(linkInjector);

            PlayerLink instance = linkInjector.getInstance(mainClass);

            // we use our own internal PlayerLinking when global linking is enabled
            if (linkConfig.isEnableGlobalLinking()) {
                GlobalPlayerLinking linking = linkInjector.getInstance(GlobalPlayerLinking.class);
                linking.setDatabaseImpl(instance);
                linking.load();
                return linking;
            } else {
                instance.load();
                return instance;
            }
        } catch (ClassCastException exception) {
            logger.error(
                    "The database implementation ({}) doesn't extend the PlayerLink class!",
                    implementationPath.getFileName().toString(), exception
            );
            return new DisabledPlayerLink();
        } catch (Exception exception) {
            if (init) {
                logger.error("Error while initialising database jar", exception);
            } else {
                logger.error("Error while loading database jar", exception);
            }
            return new DisabledPlayerLink();
        }
    }

    @Subscribe
    public void onShutdown(ShutdownEvent ignored) throws Exception {
        instance.stop();
        if (classLoader != null) {
            classLoader.close();
        }
    }
}
