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

import org.geysermc.floodgate.fabric.inject.fabric.FabricInjector;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.fabric.module.FabricAddonModule;
import org.geysermc.floodgate.fabric.module.FabricCommandModule;
import org.geysermc.floodgate.fabric.module.FabricListenerModule;
import org.geysermc.floodgate.fabric.module.FabricPlatformModule;
import org.geysermc.floodgate.module.*;

public class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricInjector.setInstance(new FabricInjector());

        Injector injector = Guice.createInjector(
                new ServerCommonModule(FabricLoader.getInstance().getConfigDir().resolve("floodgate")),
                new FabricPlatformModule()
        );

        FabricPlatform platform = injector.getInstance(FabricPlatform.class);

        platform.enable(new FabricCommandModule());

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            long ctm = System.currentTimeMillis();

            // Stupid hack, see the class for more information
            // This can probably be Guice-i-fied but that is beyond me
            MinecraftServerHolder.set(server);

            platform.enable(
                            new FabricAddonModule(),
                            new FabricListenerModule(),
                            new PluginMessageModule()
                    );

            long endCtm = System.currentTimeMillis();
            injector.getInstance(FloodgateLogger.class)
                    .translatedInfo("floodgate.core.finish", endCtm - ctm);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            platform.disable();
        });
    }
}
