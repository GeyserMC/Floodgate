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
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.module.ServerCommonModule;
import org.geysermc.floodgate.module.SpigotAddonModule;
import org.geysermc.floodgate.module.SpigotCommandModule;
import org.geysermc.floodgate.module.SpigotListenerModule;
import org.geysermc.floodgate.module.SpigotPlatformModule;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotProtocolSupportHandler;
import org.geysermc.floodgate.util.SpigotProtocolSupportListener;

public final class SpigotPlugin extends JavaPlugin {
    private SpigotPlatform platform;

    @Override
    public void onLoad() {
        String minecraftVersion = getServer().getClass().getPackage().getName().split("\\.")[3];
        ReflectionUtils.setPrefix("net.minecraft.server." + minecraftVersion);

        long ctm = System.currentTimeMillis();
        Injector injector = Guice.createInjector(
                new ServerCommonModule(getDataFolder().toPath()),
                new SpigotPlatformModule(this)
        );

        platform = injector.getInstance(SpigotPlatform.class);

        long endCtm = System.currentTimeMillis();
        injector.getInstance(FloodgateLogger.class)
                .translatedInfo("floodgate.core.finish", endCtm - ctm);
    }

    @Override
    public void onEnable() {
        platform.enable(
                new SpigotCommandModule(this),
                new SpigotListenerModule(),
                new SpigotAddonModule()
        );

        // add ProtocolSupport support (hack)
        if (getServer().getPluginManager().getPlugin("ProtocolSupport") != null) {
            InstanceHolder.getHandshakeHandlers()
                    .addHandshakeHandler(new SpigotProtocolSupportHandler());
            SpigotProtocolSupportListener.registerHack(this);
        }
    }

    @Override
    public void onDisable() {
        platform.disable();
    }
}
