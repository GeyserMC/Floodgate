package org.geysermc.floodgate.pluginmessage.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

public record FormPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, FormPayload> STREAM_CODEC = CustomPacketPayload.codec(FormPayload::write, FormPayload::new);
    public static final CustomPacketPayload.Type<FormPayload> TYPE = CustomPacketPayload.createType("floodgate:form");

    private FormPayload(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readByteArray());
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeByteArray(this.data);
    }

    public CustomPacketPayload.@NonNull Type<FormPayload> type() {
        return TYPE;
    }

    public byte[] data() {
        return this.data;
    }
}