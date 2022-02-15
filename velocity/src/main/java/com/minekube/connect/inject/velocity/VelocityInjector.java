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
import static com.minekube.connect.util.ReflectionUtils.getValue;

import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.network.netty.LocalServerChannelWrapper;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.local.LocalAddress;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityInjector extends CommonPlatformInjector {
    private final ProxyServer server;
    private final FloodgateLogger logger;

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
        ChannelInitializer<Channel> channelInitializer = castedInvoke(serverInitializerHolder,
                "get");

        // Is set on Velocity's end for listening to Java connections
        // required on ours or else the initial world load process won't finish sometimes
        WriteBufferWaterMark serverWriteMark = getCastedValue(connectionManager,
                "SERVER_WRITE_MARK");

        EventLoopGroup bossGroup = castedInvoke(connectionManager, "getBossGroup");
        EventLoopGroup workerGroup = getCastedValue(connectionManager, "workerGroup");

        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(channelInitializer)
                .group(bossGroup, workerGroup) // Cannot be DefaultEventLoopGroup
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        serverWriteMark) // Required or else rare network freezes can occur
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();

        return injected = true;
    }

    @Override
    public boolean canRemoveInjection() {
        return false;
    }

    @Override
    public boolean removeInjection() {
        logger.error("Floodgate cannot remove itself from Velocity without a reboot");
        return false;
    }

}
