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

import com.google.inject.Inject;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.geysermc.configutils.ConfigUtilities;
import org.geysermc.configutils.file.codec.PathFileCodec;
import org.geysermc.configutils.file.template.ResourceTemplateReader;
import org.geysermc.configutils.updater.change.Changes;

@Getter
@RequiredArgsConstructor
public final class ConfigLoader {
    private final Path dataFolder;
    private final Class<? extends ConnectConfig> configClass;
    private final EndpointNameGenerator endpointNameGenerator;

    private final ConnectLogger logger;

    @SuppressWarnings("unchecked")
    public <T extends ConnectConfig> T load() {
        String templateFile = "config.yml";
        if (ProxyConnectConfig.class.isAssignableFrom(configClass)) {
            templateFile = "proxy-" + templateFile;
        }

        //todo old Connect logged a message when version = 0 and it generated a new key.
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
                                .version(1, Changes.versionBuilder())
                                .build())
                        .definePlaceholder("metrics.uuid", UUID::randomUUID)
                        .definePlaceholder("endpoint", endpointNameGenerator::get)
                        .postInitializeCallbackArgument(this)
                        .saveConfigAutomatically(true)
                        .build();

        try {
            // temporary placeholder fix
            File config = new File(dataFolder.toFile(), "config.yml");
            if (config.exists()) {
                fixPlaceholderIssue(config.toPath());
            } else {
                utilities.executeOn(configClass); // create config
            }

            fixPlaceholderIssue(config.toPath()); // apply fix

            return (T) utilities.executeOn(configClass);
        } catch (Throwable throwable) {
            throw new RuntimeException(
                    "Failed to load the config! Try to delete the config file if this error persists",
                    throwable
            );
        }
    }

    private static void fixPlaceholderIssue(Path path) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset)
                .replace("${metrics.uuid}", UUID.randomUUID().toString());
        Files.write(path, content.getBytes(charset));
    }

    public static class EndpointNameGenerator {
        private static final String URL = "https://randomname.minekube.net";

        final private OkHttpClient client;

        @Inject
        public EndpointNameGenerator(OkHttpClient client) {
            this.client = client.newBuilder()
                    .callTimeout(Duration.ofSeconds(5))
                    .build();
        }

        String get() {
            Request req = new Builder()
                    .url(URL)
                    .build();

            Response res;
            try {
                res = client.newCall(req).execute();
            } catch (IOException e) {
                e.printStackTrace();
                return fallback();
            }

            if (res.code() != 200) {
                return fallback();
            }

            String name;
            try (ResponseBody body = res.body()){
                assert body != null;
                name = body.string();
            } catch (IOException e) {
                e.printStackTrace();
                return fallback();
            }

            if (name.isEmpty()) {
                return fallback();
            }

            return name;
        }

        String fallback() {
            return Utils.randomString(5);
        }
    }
}
