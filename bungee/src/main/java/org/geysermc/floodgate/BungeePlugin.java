/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.module.BungeeAddonModule;
import org.geysermc.floodgate.module.BungeeListenerModule;
import org.geysermc.floodgate.module.BungeePlatformModule;
import org.geysermc.floodgate.module.CommandModule;
import org.geysermc.floodgate.module.ProxyCommonModule;
import org.geysermc.floodgate.util.ReflectionUtils;

public final class BungeePlugin extends Plugin {
    private FloodgatePlatform platform;

    @Override
    public void onLoad() {
        ReflectionUtils.setPrefix("net.md_5.bungee");

        long ctm = System.currentTimeMillis();
        Injector injector = Guice.createInjector(
                new ProxyCommonModule(getDataFolder().toPath()),
                new BungeePlatformModule(this)
        );

        // Bungeecord doesn't have a build-in function to disable plugins,
        // so there is no need to have a custom Platform class like Spigot
        platform = injector.getInstance(FloodgatePlatform.class);

        long endCtm = System.currentTimeMillis();
        injector.getInstance(FloodgateLogger.class)
                .translatedInfo("floodgate.core.finish", endCtm - ctm);
    }

    @Override
    public void onEnable() {
        platform.enable(new CommandModule(), new BungeeListenerModule(), new BungeeAddonModule());
    }

    @Override
    public void onDisable() {
        platform.disable();
    }
}
