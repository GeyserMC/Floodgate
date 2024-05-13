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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.crypto.FloodgateDataCodec;

@Singleton
@ChannelHandler.Sharable
@SuppressWarnings("ConstantConditions")
public final class VelocityServerDataHandler extends ChannelOutboundHandlerAdapter {
    @Inject SimpleFloodgateApi api;
    @Inject FloodgateDataCodec dataCodec;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (packet instanceof HandshakePacket handshake) {
            String address = handshake.getServerAddress();

            // The connection to the backend server is not the same connection as the client to the proxy.
            // This gets the client to proxy Connection from the backend server connection.

            // get the FloodgatePlayer from the ConnectedPlayer
            MinecraftConnection minecraftConnection = (MinecraftConnection) ctx.pipeline().get("handler");
            Player velocityPlayer = (Player) minecraftConnection.getAssociation();

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

                handshake.setServerAddress(originalAddress + '\0' + encodedData + remaining);
            }

            ctx.pipeline().remove(this);
        }

        ctx.write(packet, promise);
    }
}
