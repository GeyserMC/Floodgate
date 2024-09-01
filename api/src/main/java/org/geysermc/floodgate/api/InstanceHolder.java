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

package org.geysermc.floodgate.api;

import java.util.UUID;
import lombok.Getter;
import org.geysermc.floodgate.api.event.FloodgateEventBus;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.packet.PacketHandlers;

public final class InstanceHolder {
    @Getter private static FloodgateApi api;
    @Getter private static PlayerLink playerLink;
    @Getter private static FloodgateEventBus eventBus;

    private static PlatformInjector injector;
    private static PacketHandlers packetHandlers;
    private static HandshakeHandlers handshakeHandlers;
    private static UUID storedKey;

    public static boolean set(
            FloodgateApi floodgateApi,
            PlayerLink link,
            FloodgateEventBus floodgateEventBus,
            PlatformInjector platformInjector,
            PacketHandlers packetHandlers,
            HandshakeHandlers handshakeHandlers,
            UUID key
    ) {
        if (storedKey != null) {
            if (!storedKey.equals(key)) {
                return false;
            }
        } else {
            storedKey = key;
        }

        api = floodgateApi;
        playerLink = link;
        eventBus = floodgateEventBus;
        injector = platformInjector;
        InstanceHolder.packetHandlers = packetHandlers;
        InstanceHolder.handshakeHandlers = handshakeHandlers;
        return true;
    }

    /**
     * @deprecated Injector addons will be removed with the launch of Floodgate 3.0. Please look at
     * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
     */
    @Deprecated
    public static PlatformInjector getInjector() {
        return InstanceHolder.injector;
    }

    /**
     * @deprecated Packet handlers will be removed with the launch of Floodgate 3.0. Please look at
     * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
     */
    @Deprecated
    public static PacketHandlers getPacketHandlers() {
        return InstanceHolder.packetHandlers;
    }

    /**
     * @deprecated Handshake handlers will be removed with the launch of Floodgate 3.0. Please look at
     * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
     */
    @Deprecated
    public static HandshakeHandlers getHandshakeHandlers() {
        return InstanceHolder.handshakeHandlers;
    }
}
