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

package com.minekube.connect.module;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.packet.PacketHandlers;
import com.minekube.connect.config.ConfigHolder;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.config.loader.ConfigLoader;
import com.minekube.connect.config.loader.DefaultConfigHandler;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.packet.PacketHandlersImpl;
import com.minekube.connect.util.HttpUtils;
import com.minekube.connect.util.LanguageManager;
import com.minekube.connect.util.Utils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;

@RequiredArgsConstructor
public class CommonModule extends AbstractModule {
    private final Path dataDirectory;

    @Override
    protected void configure() {
        bind(ConnectApi.class).to(SimpleConnectApi.class);
        bind(PlatformInjector.class).to(CommonPlatformInjector.class);

        bind(PacketHandlers.class).to(PacketHandlersImpl.class);
        bind(PacketHandlersImpl.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("dataDirectory")
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Provides
    @Singleton
    public ConfigHolder configHolder() {
        return new ConfigHolder();
    }

    @Provides
    @Singleton
    public ConfigLoader configLoader(
            @Named("configClass") Class<? extends ConnectConfig> configClass,
            DefaultConfigHandler defaultConfigHandler,
            ConnectLogger logger) {
        return new ConfigLoader(dataDirectory, configClass, defaultConfigHandler, logger);
    }

    @Provides
    @Singleton
    public DefaultConfigHandler defaultConfigCreator() {
        return new DefaultConfigHandler();
    }

    @Provides
    @Singleton
    public LanguageManager languageLoader(
            ConfigHolder configHolder,
            ConnectLogger logger) {
        return new LanguageManager(configHolder, logger);
    }

    @Provides
    @Singleton
    public OkHttpClient okHttpClient() throws IOException {
        Path tokenFile = dataDirectory.resolve("token.json");

        Optional<String> token = Token.load(tokenFile);
        if (!token.isPresent()) {
            // Generate and save new token
            String t = Token.generate();
            Token.save(tokenFile, t);
            token = Optional.of(t);
        }
        final String apiToken = token.get();

        return HttpUtils.defaultOkHttpClient().newBuilder()
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        // Add authorization token to every request
                        .addHeader("Authorization", "Bearer " + apiToken)
                        .build()))
                .build();
    }

    @RequiredArgsConstructor
    private static class Token {
        @SerializedName("token") final String token;

        static Optional<String> load(Path tokenFile) throws IOException {
            if (Files.exists(tokenFile)) {
                // Read existing token file
                try (Reader reader = Files.newBufferedReader(tokenFile)) {
                    return Optional.ofNullable(new Gson().fromJson(reader, Token.class))
                            .map(t -> t.token);
                }
            }
            return Optional.empty();
        }

        static void save(Path tokenFile, String token) throws IOException {
            checkNotNull(tokenFile);
            checkNotNull(token);
            try (Writer writer = new FileWriter(tokenFile.toFile())) {
                new Gson().toJson(new Token(token), writer);
            }
        }

        static String generate() {
            return "T-" + Utils.randomSecureString(20);
        }
    }
}

