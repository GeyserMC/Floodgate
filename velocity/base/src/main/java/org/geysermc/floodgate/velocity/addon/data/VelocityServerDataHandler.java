/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.velocity.addon.data;

import static java.util.Objects.requireNonNull;
import static org.geysermc.floodgate.core.util.ReflectionUtils.castedInvoke;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.core.util.ReflectionUtils.invoke;
import static org.geysermc.floodgate.core.util.ReflectionUtils.setValue;

import com.velocitypowered.api.proxy.Player;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.crypto.FloodgateDataCodec;

@Singleton
@ChannelHandler.Sharable
@SuppressWarnings("ConstantConditions")
public final class VelocityServerDataHandler extends ChannelOutboundHandlerAdapter {
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_ADDRESS;
    private static final Method GET_ASSOCIATION;
    private static final Method GET_PLAYER;

    static {
        HANDSHAKE_PACKET = getPrefixedClass("protocol.packet.Handshake");
        requireNonNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        requireNonNull(HANDSHAKE_ADDRESS, "Address field of the Handshake packet cannot be null");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");

        GET_ASSOCIATION = getMethod(minecraftConnection, "getAssociation");
        requireNonNull(GET_ASSOCIATION, "getAssociation in MinecraftConnection cannot be null");

        Class<?> serverConnection = getPrefixedClass("connection.backend.VelocityServerConnection");

        GET_PLAYER = getMethod(serverConnection, "getPlayer");
        requireNonNull(GET_PLAYER, "getPlayer in VelocityServerConnection cannot be null");
    }

    @Inject SimpleFloodgateApi api;
    @Inject FloodgateDataCodec dataCodec;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (HANDSHAKE_PACKET.isInstance(packet)) {
            String address = getCastedValue(packet, HANDSHAKE_ADDRESS);

            // The connection to the backend server is not the same connection as the client to the proxy.
            // This gets the client to proxy Connection from the backend server connection.

            // get the FloodgatePlayer from the ConnectedPlayer
            Object minecraftConnection = ctx.pipeline().get("handler");
            Object association = invoke(minecraftConnection, GET_ASSOCIATION);
            Player velocityPlayer = castedInvoke(association, GET_PLAYER);

            Connection connection = api.connectionByPlatformIdentifier(velocityPlayer);
            if (connection != null) {
                // Player is a Floodgate player
                String encodedData = dataCodec.encodeToString((FloodgateConnection) connection);

                // use the same system that we use on bungee, our data goes before all the other data
                int addressFinished = address.indexOf('\0');
                String originalAddress;
                String remaining;
                if (addressFinished == -1) {
                    // There is no additional data to hook onto.
                    // this is the case for 'no forwarding' and 'modern forwarding'
                    originalAddress = address;
                    remaining = "";
                } else {
                    originalAddress = address.substring(0, addressFinished);
                    remaining = address.substring(addressFinished);
                }

                setValue(packet, HANDSHAKE_ADDRESS, originalAddress + '\0' + encodedData + remaining);
            }

            ctx.pipeline().remove(this);
        }

        ctx.write(packet, promise);
    }
}
