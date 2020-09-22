/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.inject.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.inject.CommonPlatformInjector;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Method;

import static org.geysermc.floodgate.util.ReflectionUtils.*;

@RequiredArgsConstructor
public final class VelocityInjector extends CommonPlatformInjector {
    private final ProxyServer server;
    @Getter private boolean injected = false;

    @SuppressWarnings("rawtypes")
    public boolean inject() {
        if (isInjected()) return true;

        Object connectionManager = getValue(server, "cm");

        // Client <-> Proxy

        Object serverInitializerHolder = getValue(connectionManager, "serverChannelInitializer");
        ChannelInitializer serverInitializer = castedInvoke(serverInitializerHolder, "get");

        Method serverSetter = getMethod(serverInitializerHolder, "set", ChannelInitializer.class);
        invoke(serverInitializerHolder, serverSetter,
                new VelocityChannelInitializer(this, serverInitializer, false));

        // Proxy <-> Server

        Object backendInitializerHolder = getValue(connectionManager,"backendChannelInitializer");
        ChannelInitializer backendInitializer = castedInvoke(backendInitializerHolder, "get");

        Method backendSetter = getMethod(backendInitializerHolder, "set", ChannelInitializer.class);
        invoke(backendInitializerHolder, backendSetter,
                new VelocityChannelInitializer(this, backendInitializer, true));
        return injected = true;
    }

    @Override
    public boolean removeInjection() throws Exception {
        //todo implement injection removal support
        throw new OperationNotSupportedException(
                "Floodgate cannot remove the Velocity injection at the moment");
    }

    @RequiredArgsConstructor
    @SuppressWarnings("rawtypes")
    private static final class VelocityChannelInitializer extends ChannelInitializer<Channel> {
        private static final Method initChannel;

        private final VelocityInjector injector;
        private final ChannelInitializer original;
        private final boolean proxyToServer;

        @Override
        protected void initChannel(Channel channel) {
            invoke(original, initChannel, channel);

            injector.injectAddonsCall(channel, proxyToServer);
            injector.addInjectedClient(channel);
        }

        static {
            initChannel = getMethod(ChannelInitializer.class, "initChannel", Channel.class);
        }
    }
}
