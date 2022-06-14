package org.geysermc.floodgate.pluginmessage;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.mixin.ChunkMapMixin;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinData;

public final class FabricSkinApplier implements SkinApplier {
    // See FabricCommandUtil
    private static MinecraftServer SERVER;

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, SkinData skinData) {
        SERVER.execute(() -> {
            ServerPlayer bedrockPlayer = SERVER.getPlayerList().getPlayer(floodgatePlayer.getCorrectUniqueId());
            if (bedrockPlayer == null) {
                // Disconnected probably?
                return;
            }

            // Apply the new skin internally
            PropertyMap properties = bedrockPlayer.getGameProfile().getProperties();

            properties.removeAll("textures");
            properties.put("textures", new Property("textures", skinData.getValue(), skinData.getSignature()));

            ChunkMap tracker = ((ServerLevel) bedrockPlayer.level).getChunkSource().chunkMap;
            ChunkMap.TrackedEntity entry = ((ChunkMapMixin) tracker).getEntityMap().get(bedrockPlayer.getId());
            entry.broadcastRemoved();

            // Skin is applied - now it's time to refresh the player for everyone.
            for (ServerPlayer otherPlayer : SERVER.getPlayerList().getPlayers()) {
                if (otherPlayer == bedrockPlayer) {
                    continue;
                }

                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, bedrockPlayer));
                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, bedrockPlayer));
                if (bedrockPlayer.level == otherPlayer.level) {
                    entry.updatePlayer(otherPlayer);
                }
            }
        });
    }

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }
}
