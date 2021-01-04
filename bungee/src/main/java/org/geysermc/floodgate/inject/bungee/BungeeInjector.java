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
import java.util.function.Consumer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.ReflectionUtils;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector {
    private final FloodgateLogger logger;
    @Getter private boolean injected;

    @Override
    public boolean inject() {
        try {
            // short version: needed a reliable way to access the encoder and decoder before the
            // handshake packet.

            ClassPool classPool = ClassPool.getDefault();

            CtClass handlerBossClass = classPool.get("net.md_5.bungee.netty.HandlerBoss");

            // create a new field that we can access
            CtField channelConsumerField = new CtField(
                    classPool.get("java.util.function.Consumer"), "channelConsumer",
                    handlerBossClass
            );
            channelConsumerField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            handlerBossClass.addField(channelConsumerField);

            // edit a method to call the new field when we need it
            CtMethod channelActiveMethod = handlerBossClass.getMethod(
                    "channelActive", "(Lio/netty/channel/ChannelHandlerContext;)V");
            channelActiveMethod.insertBefore(
                    "{if (handler != null) {channelConsumer.accept(ctx.channel());}}");

            Class<?> clazz = handlerBossClass.toClass();

            Consumer<Channel> channelConsumer = channel ->
                    injectClient(channel, channel.parent() != null);

            // set the field we just made
            ReflectionUtils.setValue(
                    null,
                    ReflectionUtils.getField(clazz, "channelConsumer"),
                    channelConsumer
            );

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
    }
}
