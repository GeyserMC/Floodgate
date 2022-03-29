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

package com.minekube.connect.config;

import com.minekube.connect.api.logger.ConnectLogger;
import java.nio.file.Path;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.configutils.ConfigUtilities;
import org.geysermc.configutils.file.codec.PathFileCodec;
import org.geysermc.configutils.file.template.ResourceTemplateReader;
import org.geysermc.configutils.updater.change.Changes;

@Getter
@RequiredArgsConstructor
public final class ConfigLoader {
    private final Path dataFolder;
    private final Class<? extends ConnectConfig> configClass;

    private final ConnectLogger logger;

    @SuppressWarnings("unchecked")
    public <T extends ConnectConfig> T load() {
        String templateFile = "config.yml";
        if (ProxyConnectConfig.class.isAssignableFrom(configClass)) {
            templateFile = "proxy-" + templateFile;
        }

        //todo old Floodgate logged a message when version = 0 and it generated a new key.
        // Might be nice to allow you to run a function for a specific version.

        // it would also be nice to have sections in versionBuilder so that you don't have to
        // provide the path all the time

        ConfigUtilities utilities =
                ConfigUtilities.builder()
                        .fileCodec(PathFileCodec.of(dataFolder))
                        .configFile("config.yml")
                        .templateReader(ResourceTemplateReader.of(getClass()))
                        .template(templateFile)
                        .changes(Changes.builder()
                                .build())
                        .definePlaceholder("metrics.uuid", UUID::randomUUID)
                        .postInitializeCallbackArgument(this)
                        .build();

        try {
            return (T) utilities.executeOn(configClass);
        } catch (Throwable throwable) {
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file if this error persists",
                    throwable
            );
        }
    }
}
