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

package com.minekube.connect;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.module.PaperListenerModule;
import com.minekube.connect.module.ServerCommonModule;
import com.minekube.connect.module.SpigotAddonModule;
import com.minekube.connect.module.SpigotCommandModule;
import com.minekube.connect.module.SpigotListenerModule;
import com.minekube.connect.module.SpigotPlatformModule;
import com.minekube.connect.module.WatcherModule;
import com.minekube.connect.util.ReflectionUtils;
import com.minekube.connect.util.SpigotProtocolSupportHandler;
import com.minekube.connect.util.SpigotProtocolSupportListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotPlugin extends JavaPlugin {
    private SpigotPlatform platform;
    private Injector injector;

    @Override
    public void onLoad() {
        long ctm = System.currentTimeMillis();
        injector = Guice.createInjector(
                new ServerCommonModule(getDataFolder().toPath()),
                new SpigotPlatformModule(this)
        );

        platform = injector.getInstance(SpigotPlatform.class);

        long endCtm = System.currentTimeMillis();
        injector.getInstance(ConnectLogger.class)
                .translatedInfo("connect.core.finish", endCtm - ctm);
    }

    @Override
    public void onEnable() {
        boolean usePaperListener = ReflectionUtils.getClassSilently(
                "com.destroystokyo.paper.event.profile.PreFillProfileEvent") != null;

        platform.enable(
                new SpigotCommandModule(this),
                new SpigotAddonModule(),
                (usePaperListener ? new PaperListenerModule() : new SpigotListenerModule()),
                new WatcherModule()
        );

        //todo add proper support for disabling things on shutdown and enabling this on enable

        // add ProtocolSupport support (hack)
        if (isProtocolSupport()) {
            injector.getInstance(SpigotProtocolSupportHandler.class);
            SpigotProtocolSupportListener.registerHack(this);
        }
    }

    public static boolean isProtocolSupport() {
        return Bukkit.getServer().getPluginManager().getPlugin("ProtocolSupport") != null;
    }

    @Override
    public void onDisable() {
        platform.disable();
    }
}
