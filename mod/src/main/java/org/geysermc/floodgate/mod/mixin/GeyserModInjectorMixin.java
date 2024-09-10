package org.geysermc.floodgate.mod.mixin;

import io.netty.channel.ChannelFuture;
import org.geysermc.floodgate.mod.inject.ModInjector;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.platform.mod.GeyserModInjector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GeyserModInjector.class, remap = false)
public class GeyserModInjectorMixin {

    @Shadow
    private List<ChannelFuture> allServerChannels;

    @Inject(method = "initializeLocalChannel0", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    public void floodgate$onChannelAdd(GeyserBootstrap bootstrap, CallbackInfo ci) {
        ModInjector.INSTANCE.injectClient(this.allServerChannels.get(this.allServerChannels.size() - 1));
    }
}
