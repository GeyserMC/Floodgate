package org.geysermc.floodgate;

import org.geysermc.floodgate.inject.fabric.FabricInjector;
import org.geysermc.floodgate.module.FabricAddonModule;
import org.geysermc.floodgate.module.FabricCommandModule;
import org.geysermc.floodgate.module.FabricPlatformModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.module.ServerCommonModule;

public class FabricMod implements ModInitializer {
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        FabricInjector.setInstance(new FabricInjector());

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            long ctm = System.currentTimeMillis();
            this.server = server;

            FabricServerAudiences adventure = FabricServerAudiences.of(server);

            Injector injector = Guice.createInjector(
                    new ServerCommonModule(FabricLoader.getInstance().getConfigDir().resolve("floodgate")),
                    new FabricPlatformModule(this.server, adventure)
            );

            injector.getInstance(FabricPlatform.class)
                    .enable(
                            new FabricAddonModule(),
                            new FabricCommandModule()
                    );

            long endCtm = System.currentTimeMillis();
            injector.getInstance(FloodgateLogger.class)
                    .translatedInfo("floodgate.core.finish", endCtm - ctm);
        });
    }
}
