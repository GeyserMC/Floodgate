package org.geysermc.floodgate.mixin;

import org.geysermc.floodgate.mixin_interface.ServerLoginNetworkHandlerSetter;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin implements ServerLoginNetworkHandlerSetter {
    @Shadow
    ServerLoginNetworkHandler.State state;

    @Accessor("profile")
    public abstract void setGameProfile(GameProfile profile);

    @Override
    public void setLoginState() {
        this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
    }
}
