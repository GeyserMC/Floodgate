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

package com.minekube.connect.api.packet;

import com.minekube.connect.api.util.TriFunction;
import io.netty.channel.ChannelHandlerContext;

public interface PacketHandlers {
    /**
     * Register a specific class for a specific consumer.
     *
     * @param handler     the packet handler instance
     * @param packetClass the class to start listening for
     * @param consumer    the consumer to call once the packet has been seen
     */
    void register(
            PacketHandler handler,
            Class<?> packetClass,
            TriFunction<ChannelHandlerContext, Object, Boolean, Object> consumer);

    /**
     * Register a specific class for the given packet handler's {@link
     * PacketHandler#handle(ChannelHandlerContext, Object, boolean)}.
     *
     * @param handler     the packet handler instance
     * @param packetClass the class to start listening for
     */
    default void register(PacketHandler handler, Class<?> packetClass) {
        register(handler, packetClass, handler::handle);
    }

    /**
     * Register every packet for the given packet handler's {@link PacketHandler#handle(ChannelHandlerContext,
     * Object, boolean)}
     */
    void registerAll(PacketHandler handler);

    /**
     * Unregisters all handlers registered under the given packet handler
     *
     * @param handler the packet handler instance
     */
    void deregister(PacketHandler handler);
}
