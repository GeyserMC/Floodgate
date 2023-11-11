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

package org.geysermc.floodgate.fabric;

import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.floodgate.isolation.loader.PlatformHolder;
import org.geysermc.floodgate.isolation.loader.PlatformLoader;
import net.minecraft.server.MinecraftServer;

public final class IsolatedFabricMod implements ModInitializer {
    private PlatformHolder holder;

    @Override
    public void onInitialize() {

        // TODO: logger probably does not work, grrr
        try {
            var libsDirectory = FabricLoader.getInstance().getConfigDir().resolve("floodgate");
            holder = PlatformLoader.loadDefault(getClass().getClassLoader(), libsDirectory);
            holder.init(List.of(ModInitializer.class), List.of(this));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load Floodgate", exception);
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            onEnable();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            onDisable();
        });
    }

    public void onEnable() {
        holder.load();
        try {
            holder.enable();
        } catch (Exception exception) {
            onDisable();
            throw exception;
        }
    }

    public void onDisable() {
        holder.disable();
    }
}
