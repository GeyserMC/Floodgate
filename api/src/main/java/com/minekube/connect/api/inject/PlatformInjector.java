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

package com.minekube.connect.api.inject;

import java.net.SocketAddress;

/**
 * The global interface of all the Platform Injectors. The injector can be used for various things.
 * It is used internally for intercepting handshake/login packets and for debug mode, but there is
 * also an option to add your own addons. Note that every platform that supports netty should
 * implement this, but the platform implementation isn't required to implement this.
 */
public interface PlatformInjector {

    /**
     * @return
     */
    SocketAddress getServerSocketAddress();

    /**
     * Injects the server connection.
     *
     * @return true if the connection has successfully been injected
     * @throws Exception if something went wrong while injecting the server connection
     */
    boolean inject() throws Exception;

    /**
     * If the server connection is currently injected.
     *
     * @return true if the server connection is currently injected, returns false otherwise
     */
    boolean isInjected();

    /**
     * Adds an addon to the addon list of the Injector (the addon is called when Connect injects a
     * channel). See {@link InjectorAddon} for more info.
     *
     * @param addon the addon to add to the addon list
     * @return true if the addon has been added, false if the addon is already present
     */
    boolean addAddon(InjectorAddon addon);

    /**
     * Removes an addon from the addon list of the Injector (the addon is called when Connect
     * injects a channel). See {@link InjectorAddon} for more info.
     *
     * @param addon the class of the addon to remove from the addon list
     * @param <T>   the addon type
     * @return the removed addon instance
     */
    <T extends InjectorAddon> T removeAddon(Class<T> addon);
}
