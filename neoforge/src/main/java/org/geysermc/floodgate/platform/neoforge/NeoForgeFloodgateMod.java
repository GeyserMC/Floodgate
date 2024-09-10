package org.geysermc.floodgate.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.module.PluginMessageModule;
import org.geysermc.floodgate.core.module.ServerCommonModule;
import org.geysermc.floodgate.mod.FloodgateMod;
import org.geysermc.floodgate.mod.util.ModTemplateReader;
import org.geysermc.floodgate.platform.neoforge.module.NeoForgeCommandModule;
import org.geysermc.floodgate.platform.neoforge.module.NeoForgePlatformModule;
import org.geysermc.floodgate.platform.neoforge.pluginmessage.NeoForgePluginMessageRegistration;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Mod("floodgate")
public final class NeoForgeFloodgateMod extends FloodgateMod {

    private final ModContainer container;

    public NeoForgeFloodgateMod(IEventBus modEventBus, ModContainer container) {
        this.container = container;
        init(
            new ServerCommonModule(
                FMLPaths.CONFIGDIR.get().resolve("floodgate"),
                new ModTemplateReader()
            ),
            new NeoForgePlatformModule(),
            new NeoForgeCommandModule()
        );

        modEventBus.addListener(this::onRegisterPackets);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(this::onClientStop);
        } else {
            NeoForge.EVENT_BUS.addListener(this::onServerStop);
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        this.enable(event.getServer());
    }

    private void onClientStop(GameShuttingDownEvent ignored) {
        this.disable();
    }

    private void onServerStop(ServerStoppingEvent ignored) {
        this.disable();
    }

    private void onRegisterPackets(final RegisterPayloadHandlersEvent event) {
        // Set the registrar once we're given it - NeoForgePluginMessageRegistration was created earlier in NeoForgePlatformModule
        NeoForgePluginMessageRegistration pluginMessageRegistration = injector.getInstance(NeoForgePluginMessageRegistration.class);
        pluginMessageRegistration.setRegistrar(event.registrar("floodgate").optional());

        // We can now trigger the registering of our plugin message channels
        enable(new PluginMessageModule());
    }

    @Override
    public @Nullable Path resourcePath(String file) {
        return container.getModInfo().getOwningFile().getFile().findResource(file);
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        return container.getModInfo()
            .getOwningFile()
            .getFile()
            .getScanResult()
            .getAnnotatedBy(annotationClass, ElementType.TYPE)
                .map(annotationData -> {
                    try {
                        return Class.forName(annotationData.clazz().getClassName());
                    } catch (Exception e) {
                        injector.getInstance(FloodgateLogger.class).error(e.getMessage(), e);
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }
}
