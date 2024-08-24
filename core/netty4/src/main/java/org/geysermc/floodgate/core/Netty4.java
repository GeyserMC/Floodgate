package org.geysermc.floodgate.core;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.api.connection.Connection;

@Factory
public final class Netty4 {
    @Bean
    @Singleton
    @Named("kickMessageAttribute")
    public AttributeKey<String> kickMessageAttribute() {
        return AttributeKey.valueOf("floodgate-kick-message");
    }

    @Bean
    @Singleton
    @Named("connectionAttribute")
    public AttributeKey<Connection> connectionAttribute() {
        return AttributeKey.valueOf("floodgate-player");
    }

    /**
     * This method is used in Addons.<br> Most addons can be removed once the player associated to
     * the channel has been logged in, but they should also be removed once the inject is removed.
     * Because of how Netty works it will throw an exception and we don't want that. This method
     * removes those handlers safely.
     *
     * @param pipeline the pipeline
     * @param handler  the name of the handler to remove
     */
    public static void removeHandler(ChannelPipeline pipeline, String handler) {
        ChannelHandler channelHandler = pipeline.get(handler);
        if (channelHandler != null) {
            pipeline.remove(channelHandler);
        }
    }
}
