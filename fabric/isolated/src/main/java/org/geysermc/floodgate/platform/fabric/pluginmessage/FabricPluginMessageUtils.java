package org.geysermc.floodgate.platform.fabric.pluginmessage;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.mod.MinecraftServerHolder;
import org.geysermc.floodgate.mod.pluginmessage.payloads.FormPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.PacketPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.SkinPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.TransferPayload;

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
