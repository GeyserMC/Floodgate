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

import java.nio.file.Paths;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public final class SpigotPlugin extends JavaPlugin {
    private FloodgatePlatform platform;

    @Override
    public void onLoad() {
        platform = new SpigotPlatform(this, new LibraryManager(getClassLoader(), Paths.get("./libs"), true));
    }

    @Override
    public void onEnable() {
        platform.load();
        try {
            platform.enable();
        } catch (Exception exception) {
            Bukkit.getPluginManager().disablePlugin(this);
            throw exception;
        }
    }

    @Override
    public void onDisable() {
        platform.disable();
    }
}