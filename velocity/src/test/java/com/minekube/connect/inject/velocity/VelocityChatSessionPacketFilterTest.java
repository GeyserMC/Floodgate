package com.minekube.connect.inject.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgementPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.UnsignedPlayerCommandPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VelocityChatSessionPacketFilterTest {
    @Test
    void dropsUnknownChatSessionUpdatePacketForConnectTunnel() {
        ByteBuf packet = packet(9, 1, 2, 3);
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> 9));

        assertFalse(channel.writeInbound(packet));

        assertFalse(channel.finish());
        assertEquals(0, packet.refCnt());
    }

    @Test
    void passesOtherUnknownPacketsForConnectTunnel() {
        ByteBuf packet = packet(8, 1, 2, 3);
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> 9));

        assertTrue(channel.writeInbound(packet));

        ByteBuf inbound = channel.readInbound();
        assertSame(packet, inbound);
        assertEquals(0, inbound.readerIndex());
        inbound.release();
        assertFalse(channel.finish());
    }

    @Test
    void dropsChatAcknowledgementsForConnectTunnel() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> 9));

        assertFalse(channel.writeInbound(new ChatAcknowledgementPacket(3)));

        assertFalse(channel.finish());
    }

    @Test
    void passesChatAcknowledgementsWhenConnectionIsNotConnectTunnel() {
        ChatAcknowledgementPacket packet = new ChatAcknowledgementPacket(3);
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(false, ctx -> 9));

        assertTrue(channel.writeInbound(packet));

        assertSame(packet, channel.readInbound());
        assertFalse(channel.finish());
    }

    @Test
    void rewritesZeroTimestampSessionCommandsToUnsignedCommandsForConnectTunnel() {
        SessionPlayerCommandPacket packet =
                new SessionPlayerCommandPacket("help", Instant.parse("0001-01-01T00:00:00Z"));
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> 9));

        assertTrue(channel.writeInbound(packet));

        Object inbound = channel.readInbound();
        assertTrue(inbound instanceof UnsignedPlayerCommandPacket);
        assertEquals("help", ((UnsignedPlayerCommandPacket) inbound).getCommand());
        assertFalse(channel.finish());
    }

    @Test
    void leavesCurrentSessionCommandTimestampsUnchangedForConnectTunnel() {
        SessionPlayerCommandPacket packet =
                new SessionPlayerCommandPacket("help", Instant.parse("2026-05-12T11:51:17Z"));
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> 9));

        assertTrue(channel.writeInbound(packet));

        assertSame(packet, channel.readInbound());
        assertFalse(channel.finish());
    }

    @Test
    void passesChatSessionUpdatePacketWhenConnectionIsNotConnectTunnel() {
        ByteBuf packet = packet(9, 1, 2, 3);
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(false, ctx -> 9));

        assertTrue(channel.writeInbound(packet));

        ByteBuf inbound = channel.readInbound();
        assertSame(packet, inbound);
        inbound.release();
        assertFalse(channel.finish());
    }

    @Test
    void passesPacketsWhenResolverCannotIdentifyChatSessionPacket() {
        ByteBuf packet = packet(9, 1, 2, 3);
        EmbeddedChannel channel = new EmbeddedChannel(
                new VelocityChatSessionPacketFilter(true, ctx -> -1));

        assertTrue(channel.writeInbound(packet));

        ByteBuf inbound = channel.readInbound();
        assertSame(packet, inbound);
        inbound.release();
        assertFalse(channel.finish());
    }

    @Test
    void derivesChatSessionUpdatePacketIdFromVelocityDecoderRegistry() {
        ByteBuf packet = packet(9, 1, 2, 3);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("minecraft-decoder",
                new FakeMinecraftDecoder("PLAY", new FakeProtocolRegistry(8)));
        channel.pipeline().addLast(new VelocityChatSessionPacketFilter(true));

        assertFalse(channel.writeInbound(packet));

        assertFalse(channel.finish());
        assertEquals(0, packet.refCnt());
    }

    private static ByteBuf packet(int packetId, int... payload) {
        ByteBuf packet = Unpooled.buffer();
        writeVarInt(packet, packetId);
        for (int value : payload) {
            packet.writeByte(value);
        }
        return packet;
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        do {
            int temp = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                temp |= 0x80;
            }
            buf.writeByte(temp);
        } while (value != 0);
    }

    private static final class FakeMinecraftDecoder extends ChannelInboundHandlerAdapter {
        private final Object state;
        private final Object registry;

        private FakeMinecraftDecoder(Object state, Object registry) {
            this.state = state;
            this.registry = registry;
        }
    }

    private static final class FakeProtocolRegistry {
        private final Map<Class<?>, Integer> packetClassToId;

        private FakeProtocolRegistry(int sessionChatPacketId) {
            this.packetClassToId =
                    Collections.singletonMap(SessionPlayerChatPacket.class, sessionChatPacketId);
        }
    }
}
