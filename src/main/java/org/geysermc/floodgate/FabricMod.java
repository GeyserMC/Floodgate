package org.geysermc.floodgate;

import org.geysermc.floodgate.inject.fabric.FabricInjector;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.module.*;
import org.geysermc.floodgate.pluginmessage.FabricSkinApplier;
import org.geysermc.floodgate.util.FabricCommandUtil;

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

            FabricServerAudiences adventure = FabricServerAudiences.of(server);

            // Stupid hack, see the class for more information
            // This can probably be Guice-i-fied but that is beyond me
            FabricCommandUtil.setLaterVariables(server, adventure);
            FabricSkinApplier.setServer(server);

            platform.enable(
                            new FabricAddonModule(),
                            new FabricListenerModule()
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
