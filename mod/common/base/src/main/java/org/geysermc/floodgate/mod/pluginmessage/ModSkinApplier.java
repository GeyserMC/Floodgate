package org.geysermc.floodgate.mod.pluginmessage;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.mod.mixin.ChunkMapMixin;

import java.util.Collections;

public class ModSkinApplier implements SkinApplier {

    @Inject
    @Named("minecraftServer")
    MinecraftServer minecraftServer;

    @Override
    public void applySkin(@NonNull Connection connection, SkinApplyEvent.@NonNull SkinData skinData) {
        minecraftServer.execute(() -> {
            ServerPlayer bedrockPlayer = minecraftServer.getPlayerList()
                    .getPlayer(connection.javaUuid());
            if (bedrockPlayer == null) {
                // Disconnected probably?
                return;
            }

            // Apply the new skin internally
            PropertyMap properties = bedrockPlayer.getGameProfile().getProperties();

            properties.removeAll("textures");
            properties.put("textures", new Property("textures", skinData.value(), skinData.signature()));

            ChunkMap tracker = ((ServerLevel) bedrockPlayer.level).getChunkSource().chunkMap;
            ChunkMap.TrackedEntity entry = ((ChunkMapMixin) tracker).getEntityMap().get(bedrockPlayer.getId());
            // Skin is applied - now it's time to refresh the player for everyone.
            for (ServerPlayer otherPlayer : minecraftServer.getPlayerList().getPlayers()) {
                boolean samePlayer = otherPlayer == bedrockPlayer;
                if (!samePlayer) {
                    // TrackedEntity#broadcastRemoved doesn't actually remove them from seenBy
                    entry.removePlayer(otherPlayer);
                }

                otherPlayer.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(bedrockPlayer.getUUID())));
                otherPlayer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singletonList(bedrockPlayer)));
                if (samePlayer) {
                    continue;
                }

                if (bedrockPlayer.level == otherPlayer.level) {
                    entry.updatePlayer(otherPlayer);
                }
            }
        });
    }
}
