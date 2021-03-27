package org.geysermc.floodgate.mixin;

import org.geysermc.floodgate.mixin_interface.ServerLoginNetworkHandlerSetter;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin implements ServerLoginNetworkHandlerSetter {
    @Shadow
    private GameProfile profile;

    @Shadow
    private ServerLoginNetworkHandler.State state;

    @Override
    public void setGameProfile(GameProfile profile) {
        this.profile = profile;
    }

    @Override
    public void setLoginState() {
        this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
    }
}
