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

package com.minekube.connect.inject.velocity;

import static com.minekube.connect.util.ReflectionUtils.castedInvoke;
import static com.minekube.connect.util.ReflectionUtils.getCastedValue;
import static com.minekube.connect.util.ReflectionUtils.getMethod;
import static com.minekube.connect.util.ReflectionUtils.getValue;
import static com.minekube.connect.util.ReflectionUtils.invoke;

import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.network.netty.LocalServerChannelWrapper;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoEventLoop;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalIoHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.lang.reflect.Method;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityInjector extends CommonPlatformInjector {
    private final ProxyServer server;

    @Getter private boolean injected;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean inject() {
        if (isInjected()) {
            return true;
        }

        Object connectionManager = getValue(server, "cm");

        // Client <-> Proxy

        Object serverInitializerHolder = getValue(connectionManager, "serverChannelInitializer");
        ChannelInitializer serverInitializer = castedInvoke(serverInitializerHolder, "get");

        Method serverSetter = getMethod(serverInitializerHolder, "set", ChannelInitializer.class);
        invoke(serverInitializerHolder, serverSetter,
                new VelocityChannelInitializer(this, serverInitializer, false));

        // Proxy <-> Server
//        Object backendInitializerHolder = getValue(connectionManager, "backendChannelInitializer");
//        ChannelInitializer backendInitializer = castedInvoke(backendInitializerHolder, "get");
//
//        Method backendSetter = getMethod(backendInitializerHolder, "set", ChannelInitializer.class);
//        invoke(backendInitializerHolder, backendSetter,
//                new VelocityChannelInitializer(this, backendInitializer, true));

        // Start of logic from GeyserMC
        // https://github.com/GeyserMC/Geyser/blob/31fd57a58d19829071859ef292fee706873d31fb/bootstrap/velocity/src/main/java/org/geysermc/geyser/platform/velocity/GeyserVelocityInjector.java#L59

        // Is set on Velocity's end for listening to Java connections
        // required on ours or else the initial world load process won't finish sometimes
        WriteBufferWaterMark serverWriteMark = getCastedValue(connectionManager,
                "SERVER_WRITE_MARK");

        // Use LocalIoHandler-based event loops that are compatible with LocalServerChannel
        // while still integrating with Velocity's system (Geyser approach with ConnectWatchedSingleThreadIoEventLoop)
        EventLoopGroup velocityWorkerGroup = getCastedValue(connectionManager, "workerGroup");
        
        EventLoopGroup localBossGroup = new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory()) {
            @Override
            protected ThreadFactory newDefaultThreadFactory() {
                return new DefaultThreadFactory("Connect Local Boss Group");
            }
        };
        
        EventLoopGroup localWorkerGroup = new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory()) {
            @Override
            protected ThreadFactory newDefaultThreadFactory() {
                return new DefaultThreadFactory("Connect Local Worker Group");
            }

            @Override
            protected IoEventLoop newChild(Executor executor, IoHandlerFactory ioHandlerFactory, Object... args) {
                return new ConnectWatchedSingleThreadIoEventLoop(velocityWorkerGroup, this, executor, ioHandlerFactory);
            }
        };

        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(serverInitializer)
                .group(localBossGroup, localWorkerGroup) // Use LocalIoHandler-based event loops
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        serverWriteMark) // Required or else rare network freezes can occur
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();

        // End of logic from GeyserMC

        return injected = true;
    }

    @RequiredArgsConstructor
    @SuppressWarnings("rawtypes")
    private static final class VelocityChannelInitializer extends ChannelInitializer<Channel> {
        private static final Method initChannel;

        static {
            initChannel = getMethod(ChannelInitializer.class, "initChannel", Channel.class);
        }

        private final VelocityInjector injector;
        private final ChannelInitializer original;
        private final boolean proxyToServer;

        @Override
        protected void initChannel(Channel channel) {
            invoke(original, initChannel, channel);

            injector.injectAddonsCall(channel, proxyToServer);
            injector.addInjectedClient(channel);
        }
    }
}
