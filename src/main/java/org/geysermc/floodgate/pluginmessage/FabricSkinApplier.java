package org.geysermc.floodgate.pluginmessage;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.floodgate.MinecraftServerHolder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.mixin.ChunkMapMixin;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinData;

public final class FabricSkinApplier implements SkinApplier {

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, SkinData skinData) {
        MinecraftServerHolder.get().execute(() -> {
            System.out.println("Refreshing skins for " + floodgatePlayer);
            ServerPlayer bedrockPlayer = MinecraftServerHolder.get().getPlayerList()
                    .getPlayer(floodgatePlayer.getCorrectUniqueId());
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

            System.out.println("eeeee");

            // Skin is applied - now it's time to refresh the player for everyone.
            for (ServerPlayer otherPlayer : MinecraftServerHolder.get().getPlayerList().getPlayers()) {
                boolean samePlayer = otherPlayer == bedrockPlayer;
                if (!samePlayer) {
                    // TrackedEntity#broadcastRemoved doesn't actually remove them from seenBy
                    entry.removePlayer(otherPlayer);
                }

                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, bedrockPlayer));
                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, bedrockPlayer));
                if (samePlayer) {
                    continue;
                }

                if (bedrockPlayer.level == otherPlayer.level) {
                    System.out.println("Updating entry");
                    entry.updatePlayer(otherPlayer);
                }
            }
        });
    }
}
