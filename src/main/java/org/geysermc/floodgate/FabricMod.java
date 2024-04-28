package org.geysermc.floodgate;

import org.geysermc.floodgate.inject.fabric.FabricInjector;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.module.*;

public class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricInjector.setInstance(new FabricInjector());

        Injector injector = Guice.createInjector(
                new ServerCommonModule(FabricLoader.getInstance().getConfigDir().resolve("floodgate")),
                new FabricPlatformModule()
        );

        FloodgatePlatform platform = injector.getInstance(FloodgatePlatform.class);

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
