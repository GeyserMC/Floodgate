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
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.geysermc.floodgate.core.crypto.DataCodecType;
import org.geysermc.floodgate.core.crypto.topping.Base64Topping;
import org.geysermc.floodgate.core.crypto.topping.Topping;

@Factory
public final class CommonModule {
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
    @Singleton
    public DataCodecType codecType() {
        //todo make it a config option and remove this one
        return DataCodecType.AES;
    }

    @Bean
    @Singleton
    public Topping topping() {
        return new Base64Topping();
    }
}
