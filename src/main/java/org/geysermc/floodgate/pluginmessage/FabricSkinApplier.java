package org.geysermc.floodgate.pluginmessage;

import com.google.common.collect.Lists;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinData;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
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

            // Skin is applied - now it's time to refresh the player for everyone. Oof.
            for (ServerPlayer otherPlayer : SERVER.getPlayerList().getPlayers()) {
                if (otherPlayer == bedrockPlayer) {
                    continue;
                }

                boolean loadedInWorld = otherPlayer.getCommandSenderWorld().getEntity(bedrockPlayer.getId()) != null;
                if (loadedInWorld) {
                    // Player is loaded in this world
                    otherPlayer.connection.send(new ClientboundRemoveEntitiesPacket(bedrockPlayer.getId()));
                }
                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, bedrockPlayer));

                otherPlayer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, bedrockPlayer));
                if (loadedInWorld) {
                    // Copied from EntityTrackerEntry
                    Packet<?> spawnPacket = bedrockPlayer.getAddEntityPacket();
                    otherPlayer.connection.send(spawnPacket);
                    if (!bedrockPlayer.getEntityData().isEmpty()) {
                        otherPlayer.connection.send(new ClientboundSetEntityDataPacket(bedrockPlayer.getId(), bedrockPlayer.getEntityData(), true));
                    }

                    Collection<AttributeInstance> collection = bedrockPlayer.getAttributes().getDirtyAttributes();
                    if (!collection.isEmpty()) {
                        otherPlayer.connection.send(new ClientboundUpdateAttributesPacket(bedrockPlayer.getId(), collection));
                    }

                    otherPlayer.connection.send(new ClientboundSetEntityMotionPacket(bedrockPlayer.getId(), bedrockPlayer.getDeltaMovement()));

                    List<Pair<EquipmentSlot, ItemStack>> equipmentList = Lists.newArrayList();
                    EquipmentSlot[] slots = EquipmentSlot.values();

                    for (EquipmentSlot equipmentSlot : slots) {
                        ItemStack itemStack = bedrockPlayer.getItemBySlot(equipmentSlot);
                        if (!itemStack.isEmpty()) {
                            equipmentList.add(Pair.of(equipmentSlot, itemStack.copy()));
                        }
                    }

                    if (!equipmentList.isEmpty()) {
                        otherPlayer.connection.send(new ClientboundSetEquipmentPacket(bedrockPlayer.getId(), equipmentList));
                    }

                    for (MobEffectInstance mobEffectInstance : bedrockPlayer.getActiveEffects()) {
                        otherPlayer.connection.send(new ClientboundUpdateMobEffectPacket(bedrockPlayer.getId(), mobEffectInstance));
                    }

                    if (!bedrockPlayer.getPassengers().isEmpty()) {
                        otherPlayer.connection.send(new ClientboundSetPassengersPacket(bedrockPlayer));
                    }

                    if (bedrockPlayer.getVehicle() != null) {
                        otherPlayer.connection.send(new ClientboundSetPassengersPacket(bedrockPlayer.getVehicle()));
                    }
                }
            }
        });
    }

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }
}
