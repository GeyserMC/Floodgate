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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.core.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.core.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.core.util.ReflectionUtils;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor
public class BungeeServerDataHandler extends ChannelOutboundHandlerAdapter {
    private static final Field HANDLER;
    private static final Field USER_CONNECTION;
    private static final Field CHANNEL_WRAPPER;

    static {
        HANDLER = ReflectionUtils.getField(HandlerBoss.class, "handler");
        checkNotNull(HANDLER, "handler field cannot be null");

        USER_CONNECTION = ReflectionUtils.getField(ServerConnector.class, "user");
        checkNotNull(USER_CONNECTION, "user field cannot be null");

        CHANNEL_WRAPPER =
                ReflectionUtils.getFieldOfType(UserConnection.class, ChannelWrapper.class);
        checkNotNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");
    }

    private final ProxyFloodgateApi api;
    private final AttributeKey<FloodgatePlayer> playerAttribute;

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) {
        if (packet instanceof Handshake) {
            // get the Proxy <-> Player channel from the Proxy <-> Server channel
            HandlerBoss handlerBoss = ctx.pipeline().get(HandlerBoss.class);
            PacketHandler handler = ReflectionUtils.getCastedValue(handlerBoss, HANDLER);
            if (!(handler instanceof ServerConnector)) {
                // May be an instance of PingHandler - we don't need to deal with that
                ctx.pipeline().remove(this);
                ctx.write(packet, promise);
                return;
            }

            UserConnection connection = ReflectionUtils.getCastedValue(handler, USER_CONNECTION);
            ChannelWrapper wrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);

            FloodgatePlayer player = wrapper.getHandle().attr(playerAttribute).get();

            if (player != null) {
                BedrockData data = player.as(FloodgatePlayerImpl.class).toBedrockData();
                String encryptedData = api.createEncryptedDataString(data);

                Handshake handshake = (Handshake) packet;
                String address = handshake.getHost();

                // our data goes before all the other data
                int addressFinished = address.indexOf('\0');
                String originalAddress;
                String remaining;
                if (addressFinished != -1) {
                    originalAddress = address.substring(0, addressFinished);
                    remaining = address.substring(addressFinished);
                } else {
                    originalAddress = address;
                    remaining = "";
                }

                handshake.setHost(originalAddress + '\0' + encryptedData + remaining);
                // Bungeecord will add his data after our data
            }

            ctx.pipeline().remove(this);
        }

        ctx.write(packet, promise);
    }
}
