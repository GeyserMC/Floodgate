/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.BungeeReflectionUtils;
import org.geysermc.floodgate.util.ReflectionUtils;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector {
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
                    ReflectionUtils.getCastedValue(null, framePrepender), logger
            );

            BungeeReflectionUtils.setFieldValue(null, framePrepender, customPrepender);

            injected = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean canRemoveInjection() {
        return false;
    }

    @Override
    public boolean removeInjection() {
        logger.error("Floodgate cannot remove itself from Bungee without a reboot");
        return false;
    }

    public void injectClient(Channel channel, boolean clientToProxy) {
        injectAddonsCall(channel, !clientToProxy);
        addInjectedClient(channel);
    }

    private static final String BUNGEE_INIT = "floodgate-bungee-init";

    @RequiredArgsConstructor
    private final class BungeeCustomPrepender extends Varint21LengthFieldPrepender {
        private final Varint21LengthFieldPrepender original;
        private final FloodgateLogger logger;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            original.handlerAdded(ctx);
            // The Minecraft encoder being in the pipeline isn't present until later
            ctx.pipeline().addBefore(PipelineUtils.FRAME_DECODER, BUNGEE_INIT,
                    new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg)
                                throws Exception {
                            if (ctx.channel().pipeline().get(MinecraftEncoder.class) == null) {
                                logger.debug("Minecraft encoder class not found while " +
                                        "injecting!");
                            } else {
                                ctx.channel().pipeline().addFirst(new BungeeInjectorInitializer());
                            }
                            ctx.channel().pipeline().remove(BUNGEE_INIT);
                            super.channelRead(ctx, msg);
                        }
                    });
        }
    }

    private final class BungeeInjectorInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel channel) {
            if (!channel.isOpen()) {
                return;
            }
            injectClient(channel, channel.parent() != null);
        }
    }
}
