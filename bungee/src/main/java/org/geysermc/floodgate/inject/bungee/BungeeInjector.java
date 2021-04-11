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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
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

            // we have to remove the final modifier before asking for the value
            // because we can't remove it after we got the current value
            BungeeReflectionUtils.removeFinal(framePrepender);

            BungeeCustomPrepender customPrepender = new BungeeCustomPrepender(
                    ReflectionUtils.getCastedValue(null, framePrepender), logger
            );

            ReflectionUtils.setValue(null, framePrepender, customPrepender);

            injected = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeInjection() {
        logger.error("Floodgate cannot remove itself from Bungee without a reboot");
        return false;
    }

    public void injectClient(Channel channel, boolean clientToProxy) {
        injectAddonsCall(channel, !clientToProxy);
        addInjectedClient(channel);
        channel.closeFuture().addListener(listener -> {
            channelClosedCall(channel);
            removeInjectedClient(channel);
        });
    }

    @RequiredArgsConstructor
    private final class BungeeCustomPrepender extends Varint21LengthFieldPrepender {
        private final Varint21LengthFieldPrepender original;
        private final FloodgateLogger logger;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            original.handlerAdded(ctx);
            // we're getting called before the decoder and encoder are added.
            // we'll have to wait a while :(
            ctx.executor().execute(() -> {
                int tries = 0;
                while (ctx.channel().isOpen()) {
                    if (ctx.channel().pipeline().get(MinecraftEncoder.class) != null) {
                        logger.debug("found packet encoder :)");
                        ctx.channel().pipeline().addFirst(new BungeeInjectorInitializer());
                        return;
                    }

                    // half a second should be more than enough
                    tries++;
                    if (tries > 25) {
                        logger.debug("Failed to inject " + ctx.channel().pipeline());
                        return;
                    }

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {
                    }
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
