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

package org.geysermc.floodgate;

import com.google.inject.Module;
import java.util.List;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.module.BungeeAddonModule;
import org.geysermc.floodgate.module.BungeeListenerModule;
import org.geysermc.floodgate.module.BungeePlatformModule;
import org.geysermc.floodgate.module.CommandModule;
import org.geysermc.floodgate.module.PluginMessageModule;
import org.geysermc.floodgate.module.ProxyCommonModule;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.Utils;

public class BungeePlatform extends FloodgatePlatform {
    private final Plugin plugin;

    public BungeePlatform(Plugin floodgatePlugin) {
        this.plugin = floodgatePlugin;
        ReflectionUtils.setPrefix("net.md_5.bungee");
    }

    @Override
    protected List<Module> loadStageModules() {
        return Utils.asList(
                new ProxyCommonModule(plugin.getDataFolder().toPath()),
                new BungeePlatformModule(plugin)
        );
    }

    @Override
    protected List<Module> postEnableStageModules() {
        return Utils.asList(
                new CommandModule(),
                new BungeeListenerModule(),
                new BungeeAddonModule(),
                new PluginMessageModule()
        );
    }
}
