package org.geysermc.floodgate.pluginmessage.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

public record PacketPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, PacketPayload> STREAM_CODEC = CustomPacketPayload.codec(PacketPayload::write, PacketPayload::new);
    public static final CustomPacketPayload.Type<PacketPayload> TYPE = CustomPacketPayload.createType("floodgate:packet");

    private PacketPayload(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readByteArray());
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeByteArray(this.data);
    }

    public CustomPacketPayload.@NonNull Type<PacketPayload> type() {
        return TYPE;
    }

    public byte[] data() {
        return this.data;
    }
}