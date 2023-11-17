package org.geysermc.floodgate.mod.inject;

import io.netty.channel.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.inject.CommonPlatformInjector;

@Singleton
public class ModInjector extends CommonPlatformInjector {

    @Inject FloodgateLogger logger;

    private static ModInjector instance;

    @Override
    public void inject() throws Exception {
        // handled by Mixin
    }
    public void injectClient(ChannelFuture future) {
        future.channel().pipeline().addFirst("floodgate-init", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) throws Exception {
                super.channelRead(ctx, msg);

                Channel channel = (Channel) msg;
                channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(@NonNull Channel channel) {
                        injectAddonsCall(channel, false);
                        addInjectedClient(channel);
                        channel.closeFuture().addListener(listener -> {
                            channelClosedCall(channel);
                            removeInjectedClient(channel);
                        });
                    }
                });
            }
        });
    }

    @Override
    public boolean canRemoveInjection() {
        return false;
    }

    @Override
    public void removeInjection() throws Exception {
        // not needed
    }

    @Override
    public boolean isInjected() {
        return true; // handled by Mixin
    }

    public static ModInjector getInstance() {
        return instance;
    }

    public static void setInstance(ModInjector injector) {
        instance = injector;
    }
}
