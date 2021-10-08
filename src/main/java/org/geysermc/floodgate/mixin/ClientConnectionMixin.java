package org.geysermc.floodgate.mixin;

import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(ClientConnection.class)
public interface ClientConnectionMixin {
    @Accessor("address")
    void setAddress(SocketAddress address);
}
