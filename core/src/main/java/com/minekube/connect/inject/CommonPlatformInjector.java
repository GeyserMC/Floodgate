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

package com.minekube.connect.inject;

import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.api.inject.PlatformInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Used to inject tunneled clients directly into the server, bypassing the need to implement a
 * complete TCP connection, by creating a local channel.
 */
public abstract class CommonPlatformInjector implements PlatformInjector {
    /**
     * The local channel we can use to inject ourselves into the server without creating a TCP
     * connection.
     */
    protected ChannelFuture localChannel;
    /**
     * The LocalAddress to use to connect to the server without connecting over TCP.
     */
    @Getter protected SocketAddress serverSocketAddress;

    public void shutdown() {
        if (localChannel != null && localChannel.channel().isOpen()) {
            try {
                localChannel.channel().close().sync();
                localChannel = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (localChannel != null) {
            localChannel = null;
        }
    }

    @Getter(AccessLevel.PROTECTED)
    private final Set<Channel> injectedClients = new HashSet<>();

    private final Map<Class<?>, InjectorAddon> addons = new HashMap<>();

    protected boolean addInjectedClient(Channel channel) {
        return injectedClients.add(channel);
    }

    public boolean removeInjectedClient(Channel channel) {
        return injectedClients.remove(channel);
    }

    @Override
    public boolean addAddon(InjectorAddon addon) {
        return addons.putIfAbsent(addon.getClass(), addon) == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends InjectorAddon> T removeAddon(Class<T> addon) {
        return (T) addons.remove(addon);
    }

    /**
     * Method to loop through all the addons and call {@link InjectorAddon#onInject(Channel,
     * boolean)} if {@link InjectorAddon#shouldInject()}.
     *
     * @param channel       the channel to inject
     * @param proxyToServer true if the proxy is connecting to a server or false when the player is
     *                      connecting to the proxy or false when the platform isn't a proxy
     */
    public void injectAddonsCall(Channel channel, boolean proxyToServer) {
        for (InjectorAddon addon : addons.values()) {
            if (addon.shouldInject()) {
                addon.onInject(channel, proxyToServer);
            }
        }
    }

    /**
     * Method to loop through all the addons and call {@link InjectorAddon#onChannelClosed(Channel)}
     * if {@link InjectorAddon#shouldInject()}
     *
     * @param channel the channel that was injected
     */
    public void channelClosedCall(Channel channel) {
        for (InjectorAddon addon : addons.values()) {
            if (addon.shouldInject()) {
                addon.onChannelClosed(channel);
            }
        }
    }

    /**
     * Method to loop through all the addons and call {@link InjectorAddon#onRemoveInject(Channel)}
     * if {@link InjectorAddon#shouldInject()}.
     *
     * @param channel the channel that was injected
     */
    public void removeAddonsCall(Channel channel) {
        for (InjectorAddon addon : addons.values()) {
            if (addon.shouldInject()) {
                addon.onRemoveInject(channel);
            }
        }
    }
}
