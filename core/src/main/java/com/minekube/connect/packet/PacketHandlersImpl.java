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

package com.minekube.connect.packet;

import com.minekube.connect.api.packet.PacketHandler;
import com.minekube.connect.api.packet.PacketHandlers;
import com.minekube.connect.api.util.TriFunction;
import io.netty.channel.ChannelHandlerContext;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class PacketHandlersImpl implements PacketHandlers {
    // CopyOnWriteArraySet for the per-class fanout: reads happen on every packet
    // (hot path, must be lock-free); writes only on register/deregister.
    private final Map<PacketHandler, List<HandlerEntry>> handlers = new ConcurrentHashMap<>();
    private final Set<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> globalPacketHandlers =
            new CopyOnWriteArraySet<>();
    private final Map<Class<?>, Set<TriFunction<ChannelHandlerContext, Object, Boolean, Object>>> packetHandlers =
            new ConcurrentHashMap<>();

    @Override
    public void register(
            PacketHandler handler,
            Class<?> packetClass,
            TriFunction<ChannelHandlerContext, Object, Boolean, Object> consumer) {

        if (handler == null || packetClass == null || consumer == null) {
            return;
        }

        handlers.computeIfAbsent(handler, $ -> new CopyOnWriteArrayList<>())
                .add(new HandlerEntry(packetClass, consumer));

        packetHandlers.computeIfAbsent(packetClass, $ -> new CopyOnWriteArraySet<>(globalPacketHandlers))
                .add(consumer);
    }

    @Override
    public void registerAll(PacketHandler handler) {
        if (handler == null) {
            return;
        }

        TriFunction<ChannelHandlerContext, Object, Boolean, Object> packetHandler = handler::handle;

        handlers.computeIfAbsent(handler, $ -> new CopyOnWriteArrayList<>())
                .add(new HandlerEntry(null, packetHandler));

        globalPacketHandlers.add(packetHandler);
        for (Set<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> handle : packetHandlers.values()) {
            handle.add(packetHandler);
        }
    }

    @Override
    public void deregister(PacketHandler handler) {
        if (handler == null) {
            return;
        }

        List<HandlerEntry> values = handlers.remove(handler);
        if (values != null) {
            for (HandlerEntry value : values) {
                // registerAll() stores HandlerEntry with packetClass == null.
                // ConcurrentHashMap rejects null keys, so skip the per-class
                // lookup for global handlers (the old HashMap returned null
                // silently for the same case).
                Class<?> packetClass = value.getPacket();
                if (packetClass != null) {
                    // computeIfPresent atomically removes the entry only if it's
                    // still empty after our removal, so a concurrent register()
                    // that re-populates the set in between isn't dropped.
                    packetHandlers.computeIfPresent(packetClass, (k, set) -> {
                        set.removeIf(o -> o.equals(value.getHandler()));
                        return set.isEmpty() ? null : set;
                    });
                }

                globalPacketHandlers.remove(value.getHandler());
            }
        }
    }

    public Collection<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> getPacketHandlers(
            Class<?> packet) {
        return packetHandlers.getOrDefault(packet, Collections.emptySet());
    }

    public boolean hasHandlers() {
        return !handlers.isEmpty();
    }

    @AllArgsConstructor
    @Getter
    private final static class HandlerEntry {
        private final Class<?> packet;
        private final TriFunction<ChannelHandlerContext, Object, Boolean, Object> handler;
    }
}
