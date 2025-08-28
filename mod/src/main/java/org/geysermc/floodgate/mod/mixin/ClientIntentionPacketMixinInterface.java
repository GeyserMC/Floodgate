package org.geysermc.floodgate.mod.mixin;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientIntentionPacket.class)
public interface ClientIntentionPacketMixinInterface {

    @Accessor("hostName")
    @Mutable
    void setAddress(String address);
}
