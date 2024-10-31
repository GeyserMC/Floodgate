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

package org.geysermc.floodgate.inject.bungee;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Varint21LengthFieldExtraBufPrepender;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.BungeeReflectionUtils;
import org.geysermc.floodgate.util.ReflectionUtils;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector {
    private static final String BUNGEE_INIT = "floodgate-bungee-init";

    private final FloodgateLogger logger;
    @Getter private boolean injected;

    @Override
    public void inject() {
        // Can everyone just switch to Velocity please :)
        // :( ~BungeeCord Collaborator

        // Newer Bungee versions have a separate prepender for backend and client connections
        // this field is not touched by client -> proxy
        Field serverFramePrepender =
                ReflectionUtils.getField(PipelineUtils.class, "serverFramePrepender");
        if (serverFramePrepender != null) {
            BungeeCustomServerPrepender customServerPrepender = new BungeeCustomServerPrepender(
                    this, ReflectionUtils.castedStaticValue(serverFramePrepender)
            );
            BungeeReflectionUtils.setFieldValue(null, serverFramePrepender, customServerPrepender);
        }

        // for backwards compatibility
        Field framePrepender = ReflectionUtils.getField(PipelineUtils.class, "framePrepender");
        if (framePrepender != null) {
            logger.warn("You are running an old version of BungeeCord consider updating to a newer version");
            // Required in order to inject into both Geyser <-> proxy AND proxy <-> server
            // (Instead of just replacing the ChannelInitializer which is only called for
            // player <-> proxy)
            BungeeCustomPrepender customPrepender = new BungeeCustomPrepender(
                    this, ReflectionUtils.castedStaticValue(framePrepender)
            );

            BungeeReflectionUtils.setFieldValue(null, framePrepender, customPrepender);
        } else {
            // wrap the client -> proxy channel init because the framePrepender field was deleted
            ChannelInitializer<Channel> original = PipelineUtils.SERVER_CHILD;
            Field clientChannelInitField = ReflectionUtils.getField(
                    PipelineUtils.class, "SERVER_CHILD"
            );
            Method initChannelMethod = ReflectionUtils.getMethod(
                    original.getClass(), "initChannel", Channel.class
            );
            ChannelInitializer<Channel> wrapper = new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    ReflectionUtils.invoke(original, initChannelMethod, channel);
                    // Check if the channel is open, see #547
                    if (!channel.isOpen()) {
                        return;
                    }

                    channel.pipeline().addBefore(
                            PipelineUtils.FRAME_DECODER, BUNGEE_INIT,
                            new BungeeClientToProxyInjectInitializer(BungeeInjector.this)
                    );
                }
            };
            BungeeReflectionUtils.setFieldValue(null, clientChannelInitField, wrapper);
        }

        injected = true;
    }

    @Override
    public boolean canRemoveInjection() {
        return false;
    }

    @Override
    public void removeInjection() {
        throw new IllegalStateException(
                "Floodgate cannot remove itself from Bungee without a reboot");
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
    private static final class BungeeCustomServerPrepender
            extends Varint21LengthFieldExtraBufPrepender {
        private final BungeeInjector injector;
        private final Varint21LengthFieldExtraBufPrepender original;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            original.handlerAdded(ctx);
            // The Minecraft encoder being in the pipeline isn't present until later

            // Proxy <-> Server
            ctx.pipeline().addLast(BUNGEE_INIT, new BungeeProxyToServerInjectInitializer(injector));
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
