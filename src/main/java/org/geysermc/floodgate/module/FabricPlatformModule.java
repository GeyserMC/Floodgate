package org.geysermc.floodgate.module;

import org.geysermc.floodgate.inject.fabric.FabricInjector;
import org.geysermc.floodgate.listener.FabricEventListener;
import org.geysermc.floodgate.listener.FabricEventRegistration;
import org.geysermc.floodgate.logger.Log4jFloodgateLogger;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.pluginmessage.FabricSkinApplier;
import org.geysermc.floodgate.util.FabricCommandUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class FabricPlatformModule extends AbstractModule {
    private final MinecraftServer server;
    private final FabricServerAudiences adventure;

    @Provides
    @Singleton
    public MinecraftServer server() {
        return server;
    }

    @Provides
    @Singleton
    public FabricServerAudiences adventure() {
        return adventure;
    }

    @Provides
    @Singleton
    public FloodgateLogger floodgateLogger(LanguageManager languageManager) {
        return new Log4jFloodgateLogger(LogManager.getLogger("floodgate"), languageManager);
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new FabricCommandUtil(adventure, api, logger, languageManager, server);
    }

    @Provides
    @Singleton
    public ListenerRegistration<FabricEventListener> listenerRegistration() {
        return new FabricEventRegistration();
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector() {
        return FabricInjector.getInstance();
    }

    @Provides
    @Named("packetEncoder")
    public String packetEncoder() {
        return "encoder";
    }

    @Provides
    @Named("packetDecoder")
    public String packetDecoder() {
        return "decoder";
    }

    @Provides
    @Named("packetHandler")
    public String packetHandler() {
        return "packet_handler";
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Fabric";
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier(MinecraftServer server) {
        return new FabricSkinApplier(server);
    }
}
