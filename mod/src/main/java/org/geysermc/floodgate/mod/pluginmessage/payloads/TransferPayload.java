package org.geysermc.floodgate.mod.pluginmessage.payloads;

import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

public record TransferPayload(byte[] data) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, TransferPayload> STREAM_CODEC = CustomPacketPayload.codec(TransferPayload::write, TransferPayload::new);
    public static final CustomPacketPayload.Type<TransferPayload> TYPE = new Type<>(ResourceLocation.parse("floodgate:transfer"));

    private TransferPayload(FriendlyByteBuf friendlyByteBuf) {
        this(ByteBufUtil.getBytes(friendlyByteBuf));
        friendlyByteBuf.readerIndex(friendlyByteBuf.readerIndex() + this.data.length);
    }

    private void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBytes(this.data);
    }

    @Override
    public CustomPacketPayload.@NonNull Type<TransferPayload> type() {
        return TYPE;
    }
}
