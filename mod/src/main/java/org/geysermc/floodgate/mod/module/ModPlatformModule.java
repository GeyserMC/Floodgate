package org.geysermc.floodgate.mod.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.inject.CommonPlatformInjector;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.mod.FloodgateMod;
import org.geysermc.floodgate.mod.inject.ModInjector;
import org.geysermc.floodgate.mod.logger.Log4jFloodgateLogger;
import org.geysermc.floodgate.mod.pluginmessage.ModSkinApplier;
import org.geysermc.floodgate.mod.util.ModCommandUtil;
import org.geysermc.floodgate.mod.util.ModPlatformUtils;

@RequiredArgsConstructor
public abstract class ModPlatformModule extends AbstractModule {

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new ModCommandUtil(languageManager, api, logger);
    }

    @Override
    protected void configure() {
        bind(PlatformUtils.class).to(ModPlatformUtils.class);
        bind(Logger.class).annotatedWith(Names.named("logger")).toInstance(LogManager.getLogger("floodgate"));
        bind(FloodgateLogger.class).to(Log4jFloodgateLogger.class);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector() {
        return ModInjector.INSTANCE;
    }

    @Provides
    @Named("packetEncoder")
    public String packetEncoder() {
        return FloodgateMod.INSTANCE.isClient() ? "encoder" : "outbound_config";
    }

    @Provides
    @Named("packetDecoder")
    public String packetDecoder() {
        return FloodgateMod.INSTANCE.isClient() ? "inbound_config" : "decoder" ;
    }

    @Provides
    @Named("packetHandler")
    public String packetHandler() {
        return "packet_handler";
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier() {
        return new ModSkinApplier();
    }
}
