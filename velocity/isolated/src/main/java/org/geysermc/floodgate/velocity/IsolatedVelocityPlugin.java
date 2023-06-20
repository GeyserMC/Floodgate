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

package org.geysermc.floodgate.velocity;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.nio.file.Path;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.geysermc.floodgate.isolation.loader.PlatformHolder;
import org.geysermc.floodgate.isolation.loader.PlatformLoader;

public final class IsolatedVelocityPlugin {
    private final PlatformHolder holder;

    @Inject
    public IsolatedVelocityPlugin(Injector guice, @DataDirectory Path dataDirectory) {
        try {
            var libsDirectory = dataDirectory.resolve("libs");

            holder = PlatformLoader.loadDefault(getClass().getClassLoader(), libsDirectory);
            Injector child = guice.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(LibraryManager.class).toInstance(holder.manager());
                }
            });

            holder.platformInstance(child.getInstance(holder.platformClass()));
            holder.load();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load Floodgate", exception);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        holder.enable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        holder.disable();
    }
}
