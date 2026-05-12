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

package com.minekube.connect.addon.data;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.BitSet;

final class SpigotChatSessionPacketFilter extends ChannelInboundHandlerAdapter {
    static final String HANDLER_NAME = "connect_chat_session_filter";

    private static final String CHAT_SESSION_UPDATE_PACKET =
            "net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket";
    private static final String CHAT_ACK_PACKET =
            "net.minecraft.network.protocol.game.ServerboundChatAckPacket";
    private static final String SIGNED_COMMAND_PACKET =
            "net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket";
    private static final String UNSIGNED_COMMAND_PACKET =
            "net.minecraft.network.protocol.game.ServerboundChatCommandPacket";
    private static final String CHAT_PACKET =
            "net.minecraft.network.protocol.game.ServerboundChatPacket";
    private static final String MESSAGE_SIGNATURE =
            "net.minecraft.network.chat.MessageSignature";
    private static final String LAST_SEEN_UPDATE =
            "net.minecraft.network.chat.LastSeenMessages$Update";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object replacement = rewrite(msg);
        if (replacement == null) {
            ReferenceCountUtil.release(msg);
            return;
        }
        super.channelRead(ctx, replacement);
    }

    private static Object rewrite(Object packet) throws Exception {
        if (isInstance(CHAT_SESSION_UPDATE_PACKET, packet) || isInstance(CHAT_ACK_PACKET, packet)) {
            return null;
        }
        if (isInstance(SIGNED_COMMAND_PACKET, packet)) {
            return rewriteSignedCommand(packet);
        }
        if (isInstance(CHAT_PACKET, packet)) {
            return rewriteChatLastSeen(packet);
        }
        return packet;
    }

    private static Object rewriteSignedCommand(Object packet) throws Exception {
        String command = (String) invoke(packet, "command");
        Constructor<?> constructor = classForName(UNSIGNED_COMMAND_PACKET)
                .getConstructor(String.class);
        return constructor.newInstance(command);
    }

    private static Object rewriteChatLastSeen(Object packet) throws Exception {
        Constructor<?> constructor = classForName(CHAT_PACKET).getConstructor(
                String.class,
                Instant.class,
                long.class,
                classForName(MESSAGE_SIGNATURE),
                classForName(LAST_SEEN_UPDATE)
        );
        return constructor.newInstance(
                invoke(packet, "message"),
                invoke(packet, "timeStamp"),
                invoke(packet, "salt"),
                invoke(packet, "signature"),
                emptyLastSeenUpdate()
        );
    }

    private static Object emptyLastSeenUpdate() throws Exception {
        Constructor<?> constructor = classForName(LAST_SEEN_UPDATE)
                .getConstructor(int.class, BitSet.class, byte.class);
        return constructor.newInstance(0, new BitSet(), (byte) 0);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static boolean isInstance(String className, Object value) {
        try {
            return classForName(className).isInstance(value);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Class<?> classForName(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }
}
