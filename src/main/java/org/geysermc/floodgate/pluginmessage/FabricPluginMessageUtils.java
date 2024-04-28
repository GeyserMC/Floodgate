package org.geysermc.floodgate.pluginmessage;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.floodgate.MinecraftServerHolder;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.payloads.FormPayload;
import org.geysermc.floodgate.pluginmessage.payloads.PacketPayload;
import org.geysermc.floodgate.pluginmessage.payloads.SkinPayload;
import org.geysermc.floodgate.pluginmessage.payloads.TransferPayload;

import java.util.Objects;
import java.util.UUID;

public class FabricPluginMessageUtils extends PluginMessageUtils {

    @Override
    public boolean sendMessage(UUID uuid, String channel, byte[] data) {
        try {
            ServerPlayer player = MinecraftServerHolder.get().getPlayerList().getPlayer(uuid);
            final CustomPacketPayload payload;
            switch (channel) {
                case "floodgate:form" -> payload = new FormPayload(data);
                case "floodgate:packet" -> payload = new PacketPayload(data);
                case "floodgate:skin" -> payload = new SkinPayload(data);
                case "floodgate:transfer" -> payload = new TransferPayload(data);
                default -> throw new IllegalArgumentException("unknown channel: " + channel);
            }

            Objects.requireNonNull(player);
            ServerPlayNetworking.send(player, payload);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
