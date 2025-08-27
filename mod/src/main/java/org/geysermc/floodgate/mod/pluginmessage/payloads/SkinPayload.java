package org.geysermc.floodgate.mod.pluginmessage.payloads;

import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

public record SkinPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, SkinPayload> STREAM_CODEC = CustomPacketPayload.codec(SkinPayload::write, SkinPayload::new);
    public static final CustomPacketPayload.Type<SkinPayload> TYPE = new Type<>(ResourceLocation.parse("floodgate:skin"));

    private SkinPayload(FriendlyByteBuf friendlyByteBuf) {
        this(ByteBufUtil.getBytes(friendlyByteBuf));
        friendlyByteBuf.readerIndex(friendlyByteBuf.readerIndex() + this.data.length);
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBytes(this.data);
    }

    @Override
    public CustomPacketPayload.@NonNull Type<SkinPayload> type() {
        return TYPE;
    }
}
