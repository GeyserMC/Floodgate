package org.geysermc.floodgate.pluginmessage.payloads;

import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

public record FormPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, FormPayload> STREAM_CODEC = CustomPacketPayload.codec(FormPayload::write, FormPayload::new);
    public static final CustomPacketPayload.Type<FormPayload> TYPE = new Type<>(ResourceLocation.parse("floodgate:form"));

    private FormPayload(FriendlyByteBuf friendlyByteBuf) {
        this(ByteBufUtil.getBytes(friendlyByteBuf));
        friendlyByteBuf.readerIndex(friendlyByteBuf.readerIndex() + this.data.length);
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBytes(this.data);
    }

    @Override
    public CustomPacketPayload.@NonNull Type<FormPayload> type() {
        return TYPE;
    }
}
