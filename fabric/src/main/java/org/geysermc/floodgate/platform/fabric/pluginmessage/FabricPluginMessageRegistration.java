package org.geysermc.floodgate.platform.fabric.pluginmessage;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.mod.pluginmessage.payloads.FormPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.PacketPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.SkinPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.TransferPayload;

public class FabricPluginMessageRegistration implements PluginMessageRegistration {
    @Override
    public void register(PluginMessageChannel channel) {
        switch (channel.getIdentifier()) {
            case "floodgate:form" -> {
                PayloadTypeRegistry.playC2S().register(FormPayload.TYPE, FormPayload.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(FormPayload.TYPE, FormPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(FormPayload.TYPE,
                        ((payload, context) -> channel.handleServerCall(
                                payload.data(),
                                context.player().getUUID(),
                                context.player().getGameProfile().getName())));
            }
            case "floodgate:packet" -> {
                PayloadTypeRegistry.playC2S().register(PacketPayload.TYPE, PacketPayload.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(PacketPayload.TYPE, PacketPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(PacketPayload.TYPE,
                        ((payload, context) -> channel.handleServerCall(
                                payload.data(),
                                context.player().getUUID(),
                                context.player().getGameProfile().getName())));
            }
            case "floodgate:skin" -> {
                PayloadTypeRegistry.playC2S().register(SkinPayload.TYPE, SkinPayload.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SkinPayload.TYPE, SkinPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(SkinPayload.TYPE,
                        ((payload, context) -> channel.handleServerCall(
                                payload.data(),
                                context.player().getUUID(),
                                context.player().getGameProfile().getName())));
            }
            case "floodgate:transfer" -> {
                PayloadTypeRegistry.playC2S().register(TransferPayload.TYPE, TransferPayload.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(TransferPayload.TYPE, TransferPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(TransferPayload.TYPE,
                        ((payload, context) -> channel.handleServerCall(
                                payload.data(),
                                context.player().getUUID(),
                                context.player().getGameProfile().getName())));
            }
            default -> throw new IllegalArgumentException("unknown channel: " + channel);
        }
    }
}
