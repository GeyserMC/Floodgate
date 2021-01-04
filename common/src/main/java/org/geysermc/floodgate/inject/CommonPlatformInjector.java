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

package org.geysermc.floodgate.inject;

import io.netty.channel.Channel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.api.inject.PlatformInjector;

public abstract class CommonPlatformInjector implements PlatformInjector {
    private final Set<Channel> injectedClients = new HashSet<>();
    private final Map<Class<?>, InjectorAddon> addons = new HashMap<>();

    protected boolean addInjectedClient(Channel channel) {
        return injectedClients.add(channel);
    }

    protected boolean removeInjectedClient(Channel channel) {
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
     * Method to loop through all the addons and call {@link InjectorAddon#onLoginDone(Channel)} if
     * {@link InjectorAddon#shouldInject()}.
     *
     * @param channel the channel that was injected
     */
    public void loginSuccessCall(Channel channel) {
        for (InjectorAddon addon : addons.values()) {
            if (addon.shouldInject()) {
                addon.onLoginDone(channel);
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
