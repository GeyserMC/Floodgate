/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import static org.geysermc.floodgate.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.util.ReflectionUtils.invoke;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import com.velocitypowered.api.proxy.Player;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.BedrockData;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor
public final class VelocityServerDataHandler extends MessageToMessageEncoder<Object> {
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_ADDRESS;
    private static final Method GET_ASSOCIATION;
    private static final Method GET_PLAYER;

    static {
        HANDSHAKE_PACKET = getPrefixedClass("protocol.packet.Handshake");
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_ADDRESS, "Address field of the Handshake packet cannot be null");

        Class<?> minecraftConnection = getPrefixedClass("connection.MinecraftConnection");

        GET_ASSOCIATION = getMethod(minecraftConnection, "getAssociation");
        checkNotNull(GET_ASSOCIATION, "getAssociation in MinecraftConnection cannot be null");

        Class<?> serverConnection = getPrefixedClass("connection.backend.VelocityServerConnection");

        GET_PLAYER = getMethod(serverConnection, "getPlayer");
        checkNotNull(GET_PLAYER, "getPlayer in VelocityServerConnection cannot be null");
    }

    private final ProxyFloodgateConfig config;
    private final ProxyFloodgateApi api;
    //private final AttributeKey<FloodgatePlayer> playerAttribute;
    private boolean done;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object packet, List<Object> out) {
        ReferenceCountUtil.retain(packet);
        if (done) {
            out.add(packet);
            return;
        }

        if (!HANDSHAKE_PACKET.isInstance(packet) || !config.isSendFloodgateData()) {
            done = true;
            out.add(packet);
            return;
        }

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

        // player is not a Floodgate player
        if (player == null) {
            out.add(packet);
            return;
        }

        BedrockData data = player.as(FloodgatePlayerImpl.class).toBedrockData();
        String encryptedData = api.createEncryptedDataString(data);

        // use the same system that we use on bungee, our data goes before all the other data
        int addressFinished = address.indexOf('\0');
        String originalAddress = address.substring(0, addressFinished);
        String remaining = address.substring(addressFinished);

        setValue(packet, HANDSHAKE_ADDRESS, originalAddress + '\0' + encryptedData + remaining);

        done = true;
        out.add(packet);
    }
}
