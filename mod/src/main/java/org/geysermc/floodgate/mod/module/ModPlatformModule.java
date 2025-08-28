package org.geysermc.floodgate.mod.module;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ModPlatformModule {

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

    @Bean
    @Named("packetHandler")
    public String packetHandler() {
        return "packet_handler";
    }

    // TODO implementation name
}
