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
import static org.geysermc.floodgate.util.ReflectionUtils.castedInvoke;
import static org.geysermc.floodgate.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.util.ReflectionUtils.getClassOrFallbackPrefixed;
import static org.geysermc.floodgate.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.util.ReflectionUtils.invoke;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.BedrockData;

@SuppressWarnings("ConstantConditions")
public final class VelocityServerDataHandler extends ChannelOutboundHandlerAdapter {
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_ADDRESS;
    private static final Method GET_ASSOCIATION;
    private static final Method GET_FORWARDING_MODE;
    private static final Method GET_PLAYER;

    static {
        HANDSHAKE_PACKET = getClassOrFallbackPrefixed(
                "protocol.packet.HandshakePacket",
                "protocol.packet.Handshake"
        );
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_ADDRESS, "Address field of the Handshake packet cannot be null");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");

        GET_ASSOCIATION = getMethod(minecraftConnection, "getAssociation");
        checkNotNull(GET_ASSOCIATION, "getAssociation in MinecraftConnection cannot be null");

        Class<?> configClass = getPrefixedClass("config.VelocityConfiguration");

        GET_FORWARDING_MODE = getMethod(configClass, "getPlayerInfoForwardingMode");
        checkNotNull(GET_FORWARDING_MODE, "getPlayerInfoForwardingMode in VelocityConfiguration cannot be null");

        Class<?> serverConnection = getPrefixedClass("connection.backend.VelocityServerConnection");

        GET_PLAYER = getMethod(serverConnection, "getPlayer");
        checkNotNull(GET_PLAYER, "getPlayer in VelocityServerConnection cannot be null");
    }

    private final ProxyFloodgateApi api;
    private final boolean isModernForwarding;
    //private final AttributeKey<FloodgatePlayer> playerAttribute;

    public VelocityServerDataHandler(ProxyFloodgateApi api,
                                     ProxyServer proxy) {
        this.api = api;

        Enum<?> forwardingMode = castedInvoke(proxy.getConfiguration(), GET_FORWARDING_MODE);
        this.isModernForwarding = "MODERN".equals(forwardingMode.name());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise)
            throws Exception {
        if (HANDSHAKE_PACKET.isInstance(packet)) {
            String address = getCastedValue(packet, HANDSHAKE_ADDRESS);

            // get the FloodgatePlayer from the ConnectedPlayer
            Object minecraftConnection = ctx.pipeline().get("handler");
            Object association = invoke(minecraftConnection, GET_ASSOCIATION);
            Player velocityPlayer = castedInvoke(association, GET_PLAYER);

            //noinspection ConstantConditions
            FloodgatePlayer player = api.getPlayer(velocityPlayer.getUniqueId());

            //todo use something similar to what's written below for a more direct approach

            // get the Proxy <-> Player channel from the Proxy <-> Server channel
            //MinecraftConnection minecraftConnection = ctx.pipeline().get("handler");
            //((VelocityServerConnection) minecraftConnection.association).proxyPlayer.connection.channel

            //FloodgatePlayer player = playerChannel.attr(playerAttribute).get();
            if (player != null) {
                // Player is a Floodgate player
                BedrockData data = player.as(FloodgatePlayerImpl.class).toBedrockData();
                String encryptedData = api.createEncryptedDataString(data);

                // use the same system that we use on bungee, our data goes before all the other data
                int addressFinished = address.indexOf('\0');
                String originalAddress;
                String remaining;
                if (isModernForwarding && addressFinished == -1) {
                    // There is no additional data to hook onto
                    originalAddress = address;
                    remaining = "";
                } else {
                    originalAddress = address.substring(0, addressFinished);
                    remaining = address.substring(addressFinished);
                }

                setValue(packet, HANDSHAKE_ADDRESS, originalAddress + '\0' + encryptedData
                        + remaining);
            }

            ctx.pipeline().remove(this);
        }

        ctx.write(packet, promise);
    }
}
