package org.geysermc.floodgate.mod.pluginmessage;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.micronaut.context.BeanProvider;
import jakarta.inject.Inject;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.mod.mixin.ChunkMapMixin;

import java.util.Collections;

import static org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;

public final class ModSkinApplier implements SkinApplier {

    @Inject
    BeanProvider<MinecraftServer> server;

    @Override
    public void applySkin(@NonNull Connection connection, @NonNull SkinData skinData) {
        server.get().execute(() -> {
            ServerPlayer bedrockPlayer = server.get().getPlayerList()
                    .getPlayer(connection.javaUuid());
            if (bedrockPlayer == null) {
                // TODO apply skins with delay???
                // Disconnected probably?
                return;
            }

            // Apply the new skin internally
            PropertyMap properties = bedrockPlayer.getGameProfile().getProperties();

            properties.removeAll("textures");
            properties.put("textures", new Property("textures", skinData.value(), skinData.signature()));

            ChunkMap tracker = bedrockPlayer.level().getChunkSource().chunkMap;
            ChunkMap.TrackedEntity entry = ((ChunkMapMixin) tracker).getEntityMap().get(bedrockPlayer.getId());
            // Skin is applied - now it's time to refresh the player for everyone.
            for (ServerPlayer otherPlayer : server.get().getPlayerList().getPlayers()) {
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

                if (bedrockPlayer.level() == otherPlayer.level()) {
                    entry.updatePlayer(otherPlayer);
                }
            }
        });
    }
}
