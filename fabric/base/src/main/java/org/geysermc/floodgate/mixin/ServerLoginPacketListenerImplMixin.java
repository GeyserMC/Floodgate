package org.geysermc.floodgate.mixin;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.geysermc.floodgate.mixin_interface.ServerLoginPacketListenerSetter;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin implements ServerLoginPacketListenerSetter {
    @Shadow
    ServerLoginPacketListenerImpl.State state;

    @Accessor("gameProfile")
    public abstract void setGameProfile(GameProfile profile);

    @Override
    public void setLoginState() {
        this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
    }
}
