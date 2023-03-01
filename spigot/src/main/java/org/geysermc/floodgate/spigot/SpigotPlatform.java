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

package org.geysermc.floodgate.spigot;

import com.google.inject.Module;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.core.util.ReflectionUtils;
import org.geysermc.floodgate.module.PaperListenerModule;
import org.geysermc.floodgate.module.PluginMessageModule;
import org.geysermc.floodgate.module.SpigotAddonModule;
import org.geysermc.floodgate.module.SpigotListenerModule;
import org.geysermc.floodgate.spigot.module.SpigotCommandModule;
import org.geysermc.floodgate.spigot.module.SpigotPlatformModule;
import org.geysermc.floodgate.spigot.util.SpigotHandshakeHandler;
import org.geysermc.floodgate.spigot.util.SpigotProtocolSupportHandler;
import org.geysermc.floodgate.spigot.util.SpigotProtocolSupportListener;

public class SpigotPlatform extends FloodgatePlatform {
    private final JavaPlugin plugin;

    public SpigotPlatform(JavaPlugin floodgatePlugin) {
        this.plugin = floodgatePlugin;
    }

    @Override
    protected Module[] loadStageModules() {
        return new Module[]{
                new ServerCommonModule(plugin.getDataFolder().toPath()),
                new SpigotPlatformModule(plugin)
        };
    }

    @Override
    protected Module[] postEnableStageModules() {
        boolean usePaperListener = ReflectionUtils.getClassSilently(
                "com.destroystokyo.paper.event.profile.PreFillProfileEvent") != null;

        return new Module[]{
                new SpigotCommandModule(plugin),
                new SpigotAddonModule(),
                new PluginMessageModule(),
                (usePaperListener ? new PaperListenerModule() : new SpigotListenerModule())
        };
    }

    @Override
    public void enable() throws RuntimeException {
        super.enable();

        getGuice().getInstance(HandshakeHandlers.class)
                .addHandshakeHandler(getGuice().getInstance(SpigotHandshakeHandler.class));

        // add ProtocolSupport support (hack)
        if (plugin.getServer().getPluginManager().getPlugin("ProtocolSupport") != null) {
            getGuice().getInstance(SpigotProtocolSupportHandler.class);
            SpigotProtocolSupportListener.registerHack(plugin);
        }
    }
}
