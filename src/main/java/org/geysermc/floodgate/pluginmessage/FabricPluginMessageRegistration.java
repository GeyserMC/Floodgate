package org.geysermc.floodgate.pluginmessage;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class FabricPluginMessageRegistration implements PluginMessageRegistration {
    @Override
    public void register(PluginMessageChannel channel) {
        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation(channel.getIdentifier()),
                (server, player, handler, buf, responseSender) -> {
            System.out.println("Received channel of " + channel.getIdentifier());
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            channel.handleServerCall(bytes, player.getUUID(), player.getGameProfile().getName());
        });
    }
}
