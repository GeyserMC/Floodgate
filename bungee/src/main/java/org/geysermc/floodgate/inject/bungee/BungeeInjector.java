/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.inject.bungee;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.ReflectionUtil;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector {
    private final FloodgateLogger logger;
    @Getter
    private boolean injected;

    @Override
    public boolean inject() {
        Class<?> pipelineUtils = ReflectionUtil.getPrefixedClass("netty.PipelineUtils");
        Field framePrepender = ReflectionUtil.getField(pipelineUtils, "framePrepender");
        Object customPrepender = new CustomVarint21LengthFieldPrepender(this, logger);

        ReflectionUtil.setFinalValue(null, framePrepender, customPrepender);

        injected = true;
        return true;
    }

    @Override
    public boolean removeInjection() throws Exception {
        //todo implement injection removal support
        throw new OperationNotSupportedException(
                "Floodgate cannot remove the Bungee injection at the moment");
    }

    public void injectClient(Channel channel) {
        boolean clientToProxy = channel.parent() instanceof ServerSocketChannel;
        logger.info("Client to proxy? " + clientToProxy);

        channel.pipeline().addLast(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                injectAddonsCall(channel, !clientToProxy);
                addInjectedClient(channel);
            }
        });
    }

    @AllArgsConstructor
    @ChannelHandler.Sharable
    private static class CustomVarint21LengthFieldPrepender extends Varint21LengthFieldPrepender {
        private final BungeeInjector injector;
        private final FloodgateLogger logger;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            // we're getting called before the encoder and decoder are added,
            // so we have to wait a little, so we have a nice while loop here :D
            //todo look if we can make this nicer
            ctx.executor().execute(() -> {
                logger.debug("Channel: {} {} {}",
                        ctx.channel().isActive(),
                        ctx.channel().isOpen(),
                        ctx.channel().isRegistered()
                );

                long ctm = System.currentTimeMillis();
                while (ctx.channel().isOpen()) {
                    logger.debug("Trying to find decoder for {} {}",
                            getHostString(ctx, true),
                            getParentName(ctx, true)
                    );

                    if (ctx.channel().pipeline().get(MinecraftEncoder.class) != null) {
                        logger.debug("Found decoder for {}",
                                getHostString(ctx, true)
                        );

                        injector.injectClient(ctx.channel());
                        break;
                    }

                    if (System.currentTimeMillis() - ctm > 3000) {
                        logger.error("Failed to find decoder for client after 3 seconds!");
                    }
                }
            });
        }
    }

    public static String getHostString(ChannelHandlerContext ctx, boolean alwaysString) {
        SocketAddress address = ctx.channel().remoteAddress();
        if (address != null) {
            return ((InetSocketAddress) address).getHostString();
        }
        return alwaysString ? "null" : null;
    }

    public static String getParentName(ChannelHandlerContext ctx, boolean alwaysString) {
        Channel parent = ctx.channel().parent();
        if (parent != null) {
            return parent.getClass().getSimpleName();
        }
        return alwaysString ? "null" : null;
    }
}
