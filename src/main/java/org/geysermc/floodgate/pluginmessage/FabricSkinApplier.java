package org.geysermc.floodgate.pluginmessage;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.MathHelper;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public final class FabricSkinApplier implements SkinApplier {
    // See FabricCommandUtil
    private static MinecraftServer SERVER;

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, JsonObject skinResult) {
        SERVER.execute(() -> {
            ServerPlayerEntity bedrockPlayer = SERVER.getPlayerManager().getPlayer(floodgatePlayer.getCorrectUniqueId());
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
            for (ServerPlayerEntity otherPlayer : SERVER.getPlayerManager().getPlayerList()) {
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
                    // Copied from EntityTrackerEntry
                    Packet<?> spawnPacket = bedrockPlayer.createSpawnPacket();
                    otherPlayer.networkHandler.sendPacket(spawnPacket);
                    if (!bedrockPlayer.getDataTracker().isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(bedrockPlayer.getEntityId(), bedrockPlayer.getDataTracker(), true));
                    }

                    Collection<EntityAttributeInstance> collection = bedrockPlayer.getAttributes().getAttributesToSend();
                    if (!collection.isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityAttributesS2CPacket(bedrockPlayer.getEntityId(), collection));
                    }

                    otherPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(bedrockPlayer.getEntityId(), bedrockPlayer.getVelocity()));

                    List<Pair<EquipmentSlot, ItemStack>> equipmentList = Lists.newArrayList();
                    EquipmentSlot[] slots = EquipmentSlot.values();

                    for (EquipmentSlot equipmentSlot : slots) {
                        ItemStack itemStack = bedrockPlayer.getEquippedStack(equipmentSlot);
                        if (!itemStack.isEmpty()) {
                            equipmentList.add(Pair.of(equipmentSlot, itemStack.copy()));
                        }
                    }

                    if (!equipmentList.isEmpty()) {
                        otherPlayer.networkHandler.sendPacket(new EntityEquipmentUpdateS2CPacket(bedrockPlayer.getEntityId(), equipmentList));
                    }

                    for (StatusEffectInstance statusEffectInstance : bedrockPlayer.getStatusEffects()) {
                        otherPlayer.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(bedrockPlayer.getEntityId(), statusEffectInstance));
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
