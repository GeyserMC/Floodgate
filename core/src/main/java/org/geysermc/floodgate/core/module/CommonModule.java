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

package org.geysermc.floodgate.core.module;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.netty.util.AttributeKey;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.crypto.DataCodecType;
import org.geysermc.floodgate.core.crypto.topping.Base64Topping;
import org.geysermc.floodgate.core.crypto.topping.Topping;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.GlobalBeanCache;

@Factory
@BootstrapContextCompatible
public class CommonModule {
    @Bean(preDestroy = "shutdown")
    @Singleton
    @Named("commonPool")
    public ExecutorService commonPool() {
        return new ThreadPoolExecutor(0, 20, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    @Bean(preDestroy = "shutdown")
    @Singleton
    @Named("commonScheduledPool")
    public ScheduledExecutorService commonScheduledPool() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    @BootstrapContextCompatible
    @Singleton
    @Named("dataDirectory")
    public Path dataDirectory() {
        // todo discussion asking how you can register bootstrap context beans
        //  https://github.com/micronaut-projects/micronaut-core/discussions/9191
        return Paths.get("plugins", "floodgate");
    }

    @Bean
    @BootstrapContextCompatible
    @Singleton
    public DataCodecType codecType() {
        //todo make it a config option and remove this one
        // just like the topping it shouldn't need BootstrapContextCompatible
        return GlobalBeanCache.cacheIfAbsent("codecType", () -> DataCodecType.AES);
    }

    @Bean
    @BootstrapContextCompatible
    @Singleton
    public Topping topping() {
        return GlobalBeanCache.cacheIfAbsent("topping", Base64Topping::new);
    }

    @Bean
    @Singleton
    @Named("gitBranch")
    public String gitBranch() {
        return Constants.GIT_BRANCH;
    }

    @Bean
    @Singleton
    @Named("buildNumber")
    public int buildNumber() {
        return Constants.BUILD_NUMBER;
    }

    @Bean
    @Singleton
    @Named("kickMessageAttribute")
    public AttributeKey<String> kickMessageAttribute() {
        return AttributeKey.valueOf("floodgate-kick-message");
    }

    @Bean
    @Singleton
    @Named("connectionAttribute")
    public AttributeKey<Connection> connectionAttribute() {
        return AttributeKey.valueOf("floodgate-player");
    }
}
