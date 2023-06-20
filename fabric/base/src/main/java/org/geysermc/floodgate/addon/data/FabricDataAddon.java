package org.geysermc.floodgate.addon.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.util.Utils;

public final class FabricDataAddon implements InjectorAddon {
    @Inject private FloodgateHandshakeHandler handshakeHandler;
    @Inject private FloodgateConfig config;
    @Inject private SimpleFloodgateApi api;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    private String packetHandlerName;

    @Inject
    @Named("kickMessageAttribute")
    private AttributeKey<String> kickMessageAttribute;

    @Inject
    @Named("playerAttribute")
    private AttributeKey<FloodgatePlayer> playerAttribute;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        channel.pipeline().addBefore(
                packetHandlerName, "floodgate_data_handler",
                new FabricDataHandler(handshakeHandler, config, kickMessageAttribute, logger)
        );
    }

    @Override
    public void onChannelClosed(Channel channel) {
        FloodgatePlayer player = channel.attr(playerAttribute).get();
        if (player != null && api.setPendingRemove(player)) {
            logger.translatedInfo("floodgate.ingame.disconnect_name", player.getCorrectUsername());
        }
    }

    @Override
    public void onRemoveInject(Channel channel) {
        Utils.removeHandler(channel.pipeline(), "floodgate_data_handler");
    }

    @Override
    public boolean shouldInject() {
        return true;
    }
}
