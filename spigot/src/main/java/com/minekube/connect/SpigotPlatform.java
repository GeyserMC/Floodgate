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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotPlatform extends ConnectPlatform {
    @Inject private JavaPlugin plugin;

    @Inject
    public SpigotPlatform(ConnectApi api, PlatformInjector platformInjector,
                          ConnectLogger logger, Injector injector) {
        super(api, platformInjector, logger, injector);
    }

    @Override
    public boolean enable(Module... postInitializeModules) {
        boolean success = super.enable(postInitializeModules);
        if (!success) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }
        return true;
    }
}
