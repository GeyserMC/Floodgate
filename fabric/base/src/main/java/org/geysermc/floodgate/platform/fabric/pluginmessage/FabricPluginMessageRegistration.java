package org.geysermc.floodgate.platform.fabric.pluginmessage;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.mod.pluginmessage.payloads.FormPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.PacketPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.SkinPayload;
import org.geysermc.floodgate.mod.pluginmessage.payloads.TransferPayload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Function;

public final class FabricPluginMessageRegistration implements PluginMessageRegistration {

    @Override
    public void register(PluginMessageChannel channel) {
        final String id = channel.getIdentifier();

        switch (id) {
            case "floodgate:form" ->
                registerBoth(channel, FormPayload.TYPE, FormPayload.STREAM_CODEC, FormPayload::data);
            case "floodgate:packet" ->
                registerBoth(channel, PacketPayload.TYPE, PacketPayload.STREAM_CODEC, PacketPayload::data);
            case "floodgate:skin" ->
                registerBoth(channel, SkinPayload.TYPE, SkinPayload.STREAM_CODEC, SkinPayload::data);
            case "floodgate:transfer" ->
                registerBoth(channel, TransferPayload.TYPE, TransferPayload.STREAM_CODEC, TransferPayload::data);
            default -> throw new IllegalArgumentException("Unknown channel: " + id);
        }
    }

    /**
     * Registers payload type/codec for both directions and wires a global receiver that
     * forwards to the PluginMessageChannel.
     */
    private static <T extends CustomPacketPayload> void registerBoth(
        PluginMessageChannel channel,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        Function<T, byte[]> dataExtractor
    ) {
        // Bidirectional registration
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);

        // Single handler that delegates to channel.handleServerCall(...)
        ServerPlayNetworking.registerGlobalReceiver(
            type,
            (payload, context) -> channel.handleServerCall(
                dataExtractor.apply(payload),
                context.player().getUUID(),
                context.player().getGameProfile().getName()
            )
        );
    }
}
