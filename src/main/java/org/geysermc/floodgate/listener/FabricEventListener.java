package org.geysermc.floodgate.listener;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.LanguageManager;

public final class FabricEventListener {
    @Inject private FloodgateApi api;
    @Inject private FloodgateLogger logger;
    @Inject private LanguageManager languageManager;

    public void onPlayerJoin(ServerGamePacketListenerImpl networkHandler, PacketSender packetSender, MinecraftServer server) {
        FloodgatePlayer player = api.getPlayer(networkHandler.player.getUUID());
        if (player != null) {
            player.as(FloodgatePlayerImpl.class).setLogin(false);
            logger.translatedInfo(
                    "floodgate.ingame.login_name",
                    player.getCorrectUsername(), player.getCorrectUniqueId()
            );
            languageManager.loadLocale(player.getLanguageCode());
        }
    }
}
