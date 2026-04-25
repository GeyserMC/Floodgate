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

package org.geysermc.floodgate.api.inject;

import io.netty.channel.Channel;

/**
 * @deprecated Injector addons will be removed with the launch of Floodgate 3.0. Please look at
 * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
 */
@Deprecated
public interface InjectorAddon {
    /**
     * Called when injecting a specific channel (every client that is connected to the server has
     * his own channel). Internally used for the Floodgate debugger and data handler but can also be
     * used for third party things.
     *
     * @param channel  the channel that the injector is injecting
     * @param toServer if the connection is between a proxy and a server
     */
    void onInject(Channel channel, boolean toServer);

    /**
     * Called when the channel has been closed. Note that this method will be called for every
     * closed connection (if it is injected), so it'll also run this method for closed connections
     * between a server and the proxy (when Floodgate is running on a proxy).
     *
     * @param channel the channel that the injector injected
     */
    default void onChannelClosed(Channel channel) {
    }

    /**
     * Called when Floodgate is removing the injection from the server. The addon should remove his
     * traces otherwise it is likely that an error will popup after the server is injected again.
     *
     * @param channel the channel that the injector injected
     */
    void onRemoveInject(Channel channel);

    /**
     * If the Injector should call {@link #onInject(Channel, boolean)}
     *
     * @return true if it should, false otherwise
     */
    boolean shouldInject();
}
