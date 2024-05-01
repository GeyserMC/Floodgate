package org.geysermc.floodgate.inject.fabric;

import com.google.inject.Inject;
import io.netty.channel.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;

@RequiredArgsConstructor
public final class FabricInjector extends CommonPlatformInjector {

    @Setter @Getter
    private static FabricInjector instance;

    @Getter private final boolean injected = true;

    @Inject private FloodgateLogger logger;

    @Override
    public void inject() throws Exception {
        //no-op
    }

    public void injectClient(ChannelFuture future) {
        if (future.channel().pipeline().names().contains("floodgate-init")) {
            logger.debug("Tried to inject twice!");
            return;
        }

        future.channel().pipeline().addFirst("floodgate-init", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) throws Exception {
                super.channelRead(ctx, msg);

                if (!(msg instanceof Channel channel)) {
                    return;
                }

                channel.pipeline().addLast(new ChannelInitializer<>() {
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
    public void removeInjection() throws Exception {
        //no-op
    }

}
