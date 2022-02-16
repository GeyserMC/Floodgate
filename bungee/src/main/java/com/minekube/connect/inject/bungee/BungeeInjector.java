/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package com.minekube.connect.inject.bungee;

import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.util.BungeeReflectionUtils;
import com.minekube.connect.util.ReflectionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector {
    private static final String BUNGEE_INIT = "floodgate-bungee-init";

    private final FloodgateLogger logger;
    @Getter private boolean injected;

    @Override
    public boolean inject() {
        try {
            // Can everyone just switch to Velocity please :)

            Field framePrepender = ReflectionUtils.getField(PipelineUtils.class, "framePrepender");

            // Required in order to inject into both Geyser <-> proxy AND proxy <-> server
            // (Instead of just replacing the ChannelInitializer which is only called for
            // player <-> proxy)
            BungeeCustomPrepender customPrepender = new BungeeCustomPrepender(
                    this, ReflectionUtils.getCastedValue(null, framePrepender)
            );

            BungeeReflectionUtils.setFieldValue(null, framePrepender, customPrepender);

            injected = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    void injectClient(Channel channel, boolean clientToProxy) {
        if (!channel.isOpen()) {
            return;
        }

        if (channel.pipeline().get(MinecraftEncoder.class) == null) {
            logger.debug(
                    "Minecraft encoder not found while injecting! {}",
                    String.join(", ", channel.pipeline().names())
            );
            return;
        }

        injectAddonsCall(channel, !clientToProxy);
        addInjectedClient(channel);
    }

    @RequiredArgsConstructor
    private static final class BungeeCustomPrepender extends Varint21LengthFieldPrepender {
        private final BungeeInjector injector;
        private final Varint21LengthFieldPrepender original;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            original.handlerAdded(ctx);
            // The Minecraft encoder being in the pipeline isn't present until later

            if (ctx.channel().parent() != null) {
                // Client <-> Proxy
                ctx.pipeline().addBefore(
                        PipelineUtils.FRAME_DECODER, BUNGEE_INIT,
                        new BungeeClientToProxyInjectInitializer(injector)
                );
            } else {
                // Proxy <-> Server
                ctx.pipeline().addLast(
                        BUNGEE_INIT, new BungeeProxyToServerInjectInitializer(injector)
                );
            }
        }
    }

    @RequiredArgsConstructor
    private static final class BungeeClientToProxyInjectInitializer
            extends ChannelInboundHandlerAdapter {

        private final BungeeInjector injector;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            injector.injectClient(ctx.channel(), true);

            ctx.pipeline().remove(this);
            super.channelRead(ctx, msg);
        }
    }

    @RequiredArgsConstructor
    private static final class BungeeProxyToServerInjectInitializer
            extends ChannelOutboundHandlerAdapter {

        private final BungeeInjector injector;

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                throws Exception {
            injector.injectClient(ctx.channel(), false);

            ctx.pipeline().remove(this);
            super.write(ctx, msg, promise);
        }
    }
}
