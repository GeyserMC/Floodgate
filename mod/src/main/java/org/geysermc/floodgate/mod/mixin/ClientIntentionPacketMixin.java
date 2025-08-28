package org.geysermc.floodgate.mod.mixin;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {
    @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", constant = @Constant(intValue = 255))
    private static int floodgate$setHandshakeLength(int defaultValue) {
            return Short.MAX_VALUE;
    }
}
