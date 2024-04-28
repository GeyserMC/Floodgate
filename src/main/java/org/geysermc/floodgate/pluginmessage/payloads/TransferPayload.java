package org.geysermc.floodgate.pluginmessage.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

public record TransferPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, TransferPayload> STREAM_CODEC = CustomPacketPayload.codec(TransferPayload::write, TransferPayload::new);
    public static final CustomPacketPayload.Type<TransferPayload> TYPE = CustomPacketPayload.createType("floodgate:transfer");

    private TransferPayload(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readByteArray());
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeByteArray(this.data);
    }

    public CustomPacketPayload.@NonNull Type<TransferPayload> type() {
        return TYPE;
    }

    public byte[] data() {
        return this.data;
    }
}