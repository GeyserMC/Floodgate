package org.geysermc.floodgate.mod.inject;

import com.google.inject.Inject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.inject.CommonPlatformInjector;

@RequiredArgsConstructor
public final class ModInjector extends CommonPlatformInjector {

    public static ModInjector INSTANCE = new ModInjector();

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
