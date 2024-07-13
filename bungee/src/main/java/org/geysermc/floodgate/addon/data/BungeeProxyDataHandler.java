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

package org.geysermc.floodgate.addon.data;

import static com.google.common.base.Preconditions.checkNotNull;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.core.addon.data.CommonDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.core.util.ReflectionUtils;

@SuppressWarnings("ConstantConditions")
public class BungeeProxyDataHandler extends CommonDataHandler {
    private static final Field HANDLER;
    private static final Field CHANNEL_WRAPPER;

    static {
        HANDLER = ReflectionUtils.getField(HandlerBoss.class, "handler");
        checkNotNull(HANDLER, "handler field cannot be null");

        CHANNEL_WRAPPER =
                ReflectionUtils.getFieldOfType(InitialHandler.class, ChannelWrapper.class);
        checkNotNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");
    }

    public BungeeProxyDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            ProxyFloodgateConfig config,
            AttributeKey<String> kickMessageAttribute,
            PacketBlocker blocker) {
        super(handshakeHandler, config, kickMessageAttribute, blocker);
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
        // InitialHandler extends PacketHandler and implements PendingConnection
        InitialHandler connection = ReflectionUtils.getCastedValue(handlerBoss, HANDLER);

        ChannelWrapper channelWrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);
        channelWrapper.setRemoteAddress(newIp);
    }

    @Override
    protected Object setHostname(Object wrapperWithHandshake, String hostname) {
        PacketWrapper wrapper = (PacketWrapper) wrapperWithHandshake;
        Handshake handshake = (Handshake) wrapper.packet;
        handshake.setHost(hostname);
        return wrapper;
    }

    @Override
    public boolean channelRead(Object msg) {
        if (msg instanceof PacketWrapper) {
            DefinedPacket packet = ((PacketWrapper) msg).packet;

            // we're only interested in the Handshake packet
            if (packet instanceof Handshake) {
                handle(msg, ((Handshake) packet).getHost());

                // otherwise, it'll get read twice. once by the packet queue and once by this method
                return false;
            }
        }

        return true;
    }
}
