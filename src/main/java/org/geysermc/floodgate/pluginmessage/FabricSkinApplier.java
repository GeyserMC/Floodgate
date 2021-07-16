package org.geysermc.floodgate.pluginmessage;

import com.google.common.collect.Lists;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
            ServerPlayerEntity bedrockPlayer = SERVER.getPlayerManager().getPlayer(floodgatePlayer.getCorrectUniqueId());
            if (bedrockPlayer == null) {
                // Disconnected probably?
                return;
            }

            // Apply the new skin internally
            PropertyMap properties = bedrockPlayer.getGameProfile().getProperties();

            properties.removeAll("textures");
            properties.put("textures", new Property("textures", skinData.getValue(), skinData.getSignature()));

            // Skin is applied - now it's time to refresh the player for everyone. Oof.
            for (ServerPlayerEntity otherPlayer : SERVER.getPlayerManager().getPlayerList()) {
                if (otherPlayer == bedrockPlayer) {
                    continue;
                }

                boolean loadedInWorld = otherPlayer.getEntityWorld().getEntityById(bedrockPlayer.getId()) != null;
                if (loadedInWorld) {
                    // Player is loaded in this world
                    otherPlayer.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(bedrockPlayer.getId()));
                }
                otherPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, bedrockPlayer));

                otherPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, bedrockPlayer));
                if (loadedInWorld) {
                    // Copied from EntityTrackerEntry
                    Packet<?> spawnPacket = bedrockPlayer.createSpawnPacket();
                    otherPlayer.networkHandler.sendPacket(spawnPacket);
                    if (!bedrockPlayer.getDataTracker().isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(bedrockPlayer.getId(), bedrockPlayer.getDataTracker(), true));
                    }

                    Collection<EntityAttributeInstance> collection = bedrockPlayer.getAttributes().getAttributesToSend();
                    if (!collection.isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityAttributesS2CPacket(bedrockPlayer.getId(), collection));
                    }

                    otherPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(bedrockPlayer.getId(), bedrockPlayer.getVelocity()));

                    List<Pair<EquipmentSlot, ItemStack>> equipmentList = Lists.newArrayList();
                    EquipmentSlot[] slots = EquipmentSlot.values();

                    for (EquipmentSlot equipmentSlot : slots) {
                        ItemStack itemStack = bedrockPlayer.getEquippedStack(equipmentSlot);
                        if (!itemStack.isEmpty()) {
                            equipmentList.add(Pair.of(equipmentSlot, itemStack.copy()));
                        }
                    }

                    if (!equipmentList.isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityEquipmentUpdateS2CPacket(bedrockPlayer.getId(), equipmentList));
                    }

                    for (StatusEffectInstance statusEffectInstance : bedrockPlayer.getStatusEffects()) {
                        otherPlayer.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(bedrockPlayer.getId(), statusEffectInstance));
                    }

                    if (!bedrockPlayer.getPassengerList().isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(bedrockPlayer));
                    }

                    if (bedrockPlayer.hasVehicle()) {
                        otherPlayer.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(bedrockPlayer.getVehicle()));
                    }
                }
            }
        });
    }

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }
}
