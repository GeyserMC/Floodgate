package org.geysermc.floodgate.pluginmessage.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

public record SkinPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, SkinPayload> STREAM_CODEC = CustomPacketPayload.codec(SkinPayload::write, SkinPayload::new);
    public static final CustomPacketPayload.Type<SkinPayload> TYPE = CustomPacketPayload.createType("floodgate:skin");

    private SkinPayload(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readByteArray());
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeByteArray(this.data);
    }

    public CustomPacketPayload.@NonNull Type<SkinPayload> type() {
        return TYPE;
    }

    public byte[] data() {
        return this.data;
    }
}