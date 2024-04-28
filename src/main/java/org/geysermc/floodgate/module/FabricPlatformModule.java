package org.geysermc.floodgate.module;

import com.google.inject.name.Names;
import org.apache.logging.log4j.Logger;
import org.geysermc.floodgate.inject.fabric.FabricInjector;
import org.geysermc.floodgate.listener.FabricEventListener;
import org.geysermc.floodgate.listener.FabricEventRegistration;
import org.geysermc.floodgate.logger.Log4jFloodgateLogger;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.platform.util.PlatformUtils;
import org.geysermc.floodgate.pluginmessage.FabricPluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.FabricPluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.FabricSkinApplier;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.util.FabricCommandUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.FabricPlatformUtils;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class FabricPlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlatformUtils.class).to(FabricPlatformUtils.class);
        bind(Logger.class).annotatedWith(Names.named("logger")).toInstance(LogManager.getLogger("floodgate"));
        bind(FloodgateLogger.class).to(Log4jFloodgateLogger.class);
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new FabricCommandUtil(languageManager, api, logger);
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
    @Singleton
    public PluginMessageUtils pluginMessageUtils() {
        return new FabricPluginMessageUtils();
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Fabric";
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegister() {
        return new FabricPluginMessageRegistration();
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier() {
        return new FabricSkinApplier();
    }
}
