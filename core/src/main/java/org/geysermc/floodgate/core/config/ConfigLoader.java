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

import io.micronaut.context.ApplicationContext;
import java.nio.file.Path;
import java.util.UUID;
import org.geysermc.configutils.ConfigUtilities;
import org.geysermc.configutils.file.codec.PathFileCodec;
import org.geysermc.configutils.updater.change.Changes;

public final class ConfigLoader {
    private ConfigLoader() {}

    @SuppressWarnings("unchecked")
    public static <T extends FloodgateConfig> T load(Path dataDirectory, boolean isProxy, ApplicationContext context) {
        var configClass = isProxy ? ProxyFloodgateConfig.class : FloodgateConfig.class;

        // it would also be nice to have sections in versionBuilder so that you don't have to
        // provide the path all the time

        ConfigUtilities utilities =
                ConfigUtilities.builder()
                        .fileCodec(PathFileCodec.of(dataDirectory))
                        .configFile("config.yml")
                        .changes(Changes.builder()
                                .version(1, Changes.versionBuilder()
                                        .keyRenamed("playerLink.enable", "playerLink.enabled")
                                        .keyRenamed("playerLink.allowLinking", "playerLink.allowed"))
                                .version(2, Changes.versionBuilder()
                                        .keyRenamed("playerLink.useGlobalLinking", "playerLink.enableGlobalLinking"))
                                .version(3, Changes.versionBuilder()
                                        .keyRenamed("playerLink.type", "database.type"))
                                .build())
                        .definePlaceholder("metrics.uuid", UUID::randomUUID)
                        .postInitializeCallbackArgument(dataDirectory)
                        .build();

        T config;
        try {
            config = (T) utilities.executeOn(configClass);
        } catch (Throwable throwable) {
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file if this error persists",
                    throwable
            );
        }

        // make sure the proxy and the normal config types are registered
        context.registerSingleton(config);
        context.registerSingleton(FloodgateConfig.class, config);
        // make @Requires etc. work
        context.getEnvironment().addPropertySource(ConfigAsPropertySource.toPropertySource(config));
        return config;
    }
}
