/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.config;

import static org.spongepowered.configurate.NodePath.path;

import io.micronaut.context.ApplicationContext;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geysermc.floodgate.core.util.Constants;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class ConfigLoader {
    private ConfigLoader() {}

    @SuppressWarnings("unchecked")
    public static <T extends FloodgateConfig> T load(Path dataDirectory, boolean isProxy, ApplicationContext context) {
        var configClass = isProxy ? ProxyFloodgateConfig.class : FloodgateConfig.class;

        ConfigurationNode node;
        T config;
        try {
            var loader = YamlConfigurationLoader.builder()
                    .path(dataDirectory.resolve("config.yml"))
                    .defaultOptions(InterfaceDefaultOptions.get())
                    .build();

            node = loader.load();
            // temp fix for node.virtual() being broken
            var virtual = !Files.exists(dataDirectory.resolve("config.yml"));

                var migrations = ConfigurationTransformation.versionedBuilder()
                        .addVersion(Constants.CONFIG_VERSION, twoToThree())
                        .addVersion(2, oneToTwo())
                        .addVersion(1, zeroToOne())
                        .build();

                var startVersion = migrations.version(node);
                migrations.apply(node);
                var endVersion = migrations.version(node);

            config = (T) node.get(configClass);

            // save default config or save migrated config
            if (virtual || startVersion != endVersion) {
                loader.save(node);
            }
        } catch (ConfigurateException exception) {
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file if this error persists",
                    exception
            );
        }

        // make sure the proxy and the normal config types are registered
        context.registerSingleton(config);
        context.registerSingleton(FloodgateConfig.class, config);
        // make @Requires etc. work
        context.getEnvironment().addPropertySource(ConfigAsPropertySource.toPropertySource(node));
        return config;
    }

    private static ConfigurationTransformation zeroToOne() {
        return ConfigurationTransformation.builder()
                .addAction(path("playerLink", "enable"), (path, value) -> {
                    return new Object[]{"playerLink", "enabled"};
                })
                .addAction(path("playerLink", "allowLinking"), (path, value) -> {
                    return new Object[]{"playerLink", "allowed"};
                })
                .build();
    }

    private static ConfigurationTransformation oneToTwo() {
        return ConfigurationTransformation.builder()
                .addAction(path("playerLink", "useGlobalLinking"), (path, value) -> {
                    return new Object[]{"playerLink", "enableGlobalLinking"};
                })
                .build();
    }

    private static ConfigurationTransformation twoToThree() {
        return ConfigurationTransformation.builder()
                .addAction(path("playerLink", "type"), (path, value) -> {
                    return new Object[]{"database", "type"};
                })
                .build();
    }
}
