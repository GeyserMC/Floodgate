/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.addon.data;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;
import static org.geysermc.floodgate.util.ReflectionUtil.*;

@RequiredArgsConstructor
public class VelocityServerDataHandler extends MessageToMessageEncoder<Object> {
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_SERVER_ADDRESS;

    private final ProxyFloodgateConfig config;
    private final ProxyFloodgateApi api;

    private final Map<EventLoop, FloodgatePlayer> playerMap;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object packet, List<Object> out) {
        ReferenceCountUtil.retain(packet);
        if (!HANDSHAKE_PACKET.isInstance(packet) || !config.isSendFloodgateData()) {
            System.out.println(HANDSHAKE_PACKET.isInstance(packet)+" "+config.isSendFloodgateData());
            out.add(packet);
            return;
        }

        String address = getCastedValue(packet, HANDSHAKE_SERVER_ADDRESS);

        // works because the EventLoop is shared between the Player and the ServerConnection
        // todo check if it actually works (by using multiple accounts and log if the map
        //  overrides older ones)
        FloodgatePlayer player = playerMap.get(ctx.channel().eventLoop());

        // player is not a Floodgate player
        if (player == null) {
            System.out.println("Not Floodgate player");
            out.add(packet);
            return;
        }

        String encryptedData = api.getEncryptedData(player.getCorrectUniqueId());
        checkArgument(encryptedData != null, "Encrypted data cannot be null");

        // use the same system that we use on bungee, our data goes before all the other data
        String[] split = address.split("\0");
        String remaining = address.substring(split[0].length());

        setValue(packet, HANDSHAKE_SERVER_ADDRESS,
                split[0] + '\0' + FLOODGATE_IDENTIFIER + '\0' + encryptedData + remaining);
        System.out.println("done");
        out.add(packet);
    }

    static {
        HANDSHAKE_PACKET = getPrefixedClass("protocol.packet.Handshake");
        checkNotNull(HANDSHAKE_PACKET, "Handshake packet class cannot be null");

        HANDSHAKE_SERVER_ADDRESS = getField(HANDSHAKE_PACKET, "serverAddress");
        checkNotNull(HANDSHAKE_SERVER_ADDRESS, "Address field of the Handshake packet cannot be null");
    }
}
