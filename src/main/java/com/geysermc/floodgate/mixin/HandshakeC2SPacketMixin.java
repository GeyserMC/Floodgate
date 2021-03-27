package com.geysermc.floodgate.mixin;

import com.geysermc.floodgate.mixin_interface.HandshakeS2CPacketAddressGetter;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HandshakeC2SPacket.class)
public abstract class HandshakeC2SPacketMixin implements HandshakeS2CPacketAddressGetter {

    @Shadow private String address;

    @Override
    public String getAddress() {
        return this.address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }
}
