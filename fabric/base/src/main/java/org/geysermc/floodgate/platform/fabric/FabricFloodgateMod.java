package org.geysermc.floodgate.platform.fabric;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.geysermc.floodgate.mod.ModPlatform;

import java.nio.file.Path;

public final class FabricFloodgateMod extends ModPlatform {

    private final ModContainer container;

    public FabricFloodgateMod(LibraryManager manager, ModContainer container) {
        super(manager);
        this.container = container;
    }

    @Override
    protected void onContextCreated(ApplicationContext context) {
        super.onContextCreated(context);
        context.registerSingleton(container)
            .registerSingleton(
                Path.class,
                FabricLoader.getInstance().getConfigDir().resolve("floodgate"),
                Qualifiers.byName("dataDirectory")
        );

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            context.registerSingleton(server, false);
            context.registerSingleton(MinecraftServerAudiences.of(server), false);
        });
    }

    @Override
    public @Nullable Path resourcePath(String file) {
        return container.findPath(file).orElse(null);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}
