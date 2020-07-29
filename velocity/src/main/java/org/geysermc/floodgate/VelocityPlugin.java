/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.geysermc.floodgate.module.*;
import org.geysermc.floodgate.util.ReflectionUtil;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class VelocityPlugin {
    private final FloodgatePlatform platform;

    @Inject
    public VelocityPlugin(@DataDirectory Path dataDirectory, Injector guice, Logger logger) {
        ReflectionUtil.setPrefix("com.velocitypowered.proxy");

        long ctm = System.currentTimeMillis();
        Injector injector = guice.createChildInjector(
                new CommonModule(dataDirectory),
                new VelocityPlatformModule()
        );

        long endCtm = System.currentTimeMillis();
        logger.info("Took " + (endCtm - ctm) + "ms to boot Floodgate");

        platform = injector.getInstance(FloodgatePlatform.class);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        platform.enable(new CommandModule(), new VelocityListenerModule(),
                new VelocityAddonModule());
    }
}
