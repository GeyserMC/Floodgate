/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.loader.ConfigLoader;
import org.geysermc.floodgate.link.PlayerLinkLoader;

import java.nio.file.Path;

public final class SpigotPlatform extends FloodgatePlatform {
    @Inject private JavaPlugin plugin;

    @Inject
    public SpigotPlatform(@Named("dataDirectory") Path dataDirectory, FloodgateApi api,
                          ConfigLoader configLoader, PlayerLinkLoader playerLinkLoader,
                          HandshakeHandler handshakeHandler, FloodgateLogger logger,
                          Injector injector) {
        super(dataDirectory, api, configLoader, playerLinkLoader,
                handshakeHandler, logger, injector);
    }

    @Override
    public boolean enable(Module... postCreateModules) {
        boolean success = super.enable(postCreateModules);
        if (!success) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
        return success;
    }
}
