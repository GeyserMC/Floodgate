package org.geysermc.floodgate.pluginmessage;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;

@RequiredArgsConstructor
public class FabricSkinApplier implements SkinApplier {
    private final MinecraftServer server;

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, JsonObject skinResult) {
        this.server.execute(() -> {
            ServerPlayerEntity bedrockPlayer = this.server.getPlayerManager().getPlayer(floodgatePlayer.getCorrectUniqueId());
            if (bedrockPlayer == null) {
                // Disconnected probably?
                return;
            }

            // Apply the new skin internally
            PropertyMap properties = bedrockPlayer.getGameProfile().getProperties();

            properties.removeAll("textures");
            Property property = new Property(
                    "textures",
                    skinResult.get("value").getAsString(),
                    skinResult.get("signature").getAsString());
            properties.put("textures", property);

            // Skin is applied - now it's time to refresh the player for everyone. Oof.
            for (ServerPlayerEntity otherPlayer : this.server.getPlayerManager().getPlayerList()) {
                if (otherPlayer == bedrockPlayer) {
                    continue;
                }

                boolean loadedInWorld = otherPlayer.getEntityWorld().getEntityById(bedrockPlayer.getEntityId()) != null;
                if (loadedInWorld) {
                    // Player is loaded in this world
                    otherPlayer.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(bedrockPlayer.getEntityId()));
                }
                otherPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, bedrockPlayer));

                otherPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, bedrockPlayer));
                if (loadedInWorld) {
                    otherPlayer.networkHandler.sendPacket(new PlayerSpawnS2CPacket(bedrockPlayer));
                }
            }
        });
    }
}
