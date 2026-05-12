/*
 * Copyright (c) 2021-2022 Minekube. https://minekube.com
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
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.inject.velocity;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class VelocityChatSessionPacketFilter extends ChannelInboundHandlerAdapter {
    static final String HANDLER_NAME = "connect_chat_session_filter";

    private static final String MINECRAFT_DECODER = "minecraft-decoder";
    private static final String PLAY_STATE = "PLAY";
    private static final String SESSION_CHAT_PACKET =
            "com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket";
    private static final String SESSION_COMMAND_PACKET =
            "com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket";
    private static final String UNSIGNED_COMMAND_PACKET =
            "com.velocitypowered.proxy.protocol.packet.chat.session.UnsignedPlayerCommandPacket";
    private static final String CHAT_ACKNOWLEDGEMENT_PACKET =
            "com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgementPacket";
    private static final Instant EARLIEST_REASONABLE_CHAT_TIMESTAMP = Instant.EPOCH;
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private final boolean connectTunnel;
    private final ChatSessionPacketIdResolver resolver;

    VelocityChatSessionPacketFilter(boolean connectTunnel) {
        this(connectTunnel, VelocityChatSessionPacketFilter::resolveChatSessionPacketId);
    }

    VelocityChatSessionPacketFilter(boolean connectTunnel, ChatSessionPacketIdResolver resolver) {
        this.connectTunnel = connectTunnel;
        this.resolver = resolver;
    }

    static void inject(Channel channel, boolean connectTunnel) {
        if (!connectTunnel) {
            return;
        }

        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(MINECRAFT_DECODER) == null || pipeline.get(HANDLER_NAME) != null) {
            return;
        }

        pipeline.addAfter(MINECRAFT_DECODER, HANDLER_NAME,
                new VelocityChatSessionPacketFilter(true));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object sanitized = sanitizeCommandPacket(msg);
        if (sanitized != msg) {
            msg = sanitized;
        }

        if (connectTunnel && hasClassName(msg, CHAT_ACKNOWLEDGEMENT_PACKET)) {
            ReferenceCountUtil.release(msg);
            return;
        }

        if (connectTunnel && msg instanceof ByteBuf && shouldDrop(ctx, (ByteBuf) msg)) {
            ((ByteBuf) msg).release();
            return;
        }

        ctx.fireChannelRead(msg);
    }

    private static boolean hasClassName(Object value, String className) {
        return value != null && className.equals(value.getClass().getName());
    }

    private Object sanitizeCommandPacket(Object msg) {
        if (!connectTunnel || !hasClassName(msg, SESSION_COMMAND_PACKET)) {
            return msg;
        }

        try {
            Object timestamp = fieldValue(msg, "timeStamp");
            if (!(timestamp instanceof Instant)
                    || !((Instant) timestamp).isBefore(EARLIEST_REASONABLE_CHAT_TIMESTAMP)) {
                return msg;
            }

            Object command = fieldValue(msg, "command");
            if (!(command instanceof String)) {
                return msg;
            }

            Class<?> unsignedCommandClass = Class.forName(UNSIGNED_COMMAND_PACKET,
                    true, msg.getClass().getClassLoader());
            Object unsignedCommand = unsignedCommandClass.getDeclaredConstructor().newInstance();
            setFieldValue(unsignedCommand, "command", command);
            return unsignedCommand;
        } catch (ReflectiveOperationException | IllegalStateException ignored) {
            return msg;
        }
    }

    private boolean shouldDrop(ChannelHandlerContext ctx, ByteBuf packet) {
        int chatSessionPacketId = resolver.resolve(ctx);
        if (chatSessionPacketId < 0) {
            return false;
        }

        ByteBuf duplicate = packet.duplicate();
        int packetId = readVarInt(duplicate);
        return packetId == chatSessionPacketId;
    }

    private static int resolveChatSessionPacketId(ChannelHandlerContext ctx) {
        try {
            Object decoder = ctx.pipeline().get(MINECRAFT_DECODER);
            if (decoder == null) {
                return -1;
            }

            Object state = fieldValue(decoder, "state");
            if (!PLAY_STATE.equals(String.valueOf(state))) {
                return -1;
            }

            Object registry = fieldValue(decoder, "registry");
            Object packetClassToId = fieldValue(registry, "packetClassToId");
            if (!(packetClassToId instanceof Map)) {
                return -1;
            }

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) packetClassToId).entrySet()) {
                Object key = entry.getKey();
                if (key instanceof Class
                        && SESSION_CHAT_PACKET.equals(((Class<?>) key).getName())
                        && entry.getValue() instanceof Number) {
                    return ((Number) entry.getValue()).intValue() + 1;
                }
            }
        } catch (ReflectiveOperationException | IllegalStateException ignored) {
            return -1;
        }

        return -1;
    }

    private static Object fieldValue(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = field(target.getClass(), fieldName);
        return field.get(target);
    }

    private static void setFieldValue(Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = field(target.getClass(), fieldName);
        field.set(target, value);
    }

    private static Field field(Class<?> type, String fieldName) {
        String key = type.getName() + "#" + fieldName;
        return FIELDS.computeIfAbsent(key, $ -> {
            try {
                Class<?> current = type;
                while (current != null) {
                    try {
                        Field field = current.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        return field;
                    } catch (NoSuchFieldException ignored) {
                        current = current.getSuperclass();
                    }
                }
                throw new NoSuchFieldException(fieldName);
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private static int readVarInt(ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte currentByte;

        do {
            currentByte = buf.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
        } while ((currentByte & 0x80) != 0 && position < 35);

        return value;
    }

    interface ChatSessionPacketIdResolver {
        int resolve(ChannelHandlerContext ctx);
    }
}
