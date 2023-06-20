package org.geysermc.floodgate.mixin;

import net.minecraft.server.network.ServerConnectionListener;
import org.geysermc.floodgate.inject.fabric.FabricInjector;
import io.netty.channel.ChannelFuture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.util.List;

@Mixin(ServerConnectionListener.class)
public abstract class ServerConnectionListenerMixin {

    @Shadow @Final private List<ChannelFuture> channels;

    @Inject(method = "startTcpServerListener", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    public void onChannelAdd(InetAddress address, int port, CallbackInfo ci) {
        FabricInjector.getInstance().injectClient(this.channels.get(this.channels.size() - 1));
    }
}
