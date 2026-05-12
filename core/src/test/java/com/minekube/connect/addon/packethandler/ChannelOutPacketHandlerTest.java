package com.minekube.connect.addon.packethandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minekube.connect.api.packet.PacketHandler;
import com.minekube.connect.packet.PacketHandlersImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

class ChannelOutPacketHandlerTest {
    @Test
    void noHandlersRegisteredPassesThroughOriginalMessage() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        String msg = "foo";

        Object outbound = writeOutbound(packetHandlers, msg);

        assertSame(msg, outbound);
    }

    @Test
    void handlerReturningSameReferencePassesThroughOriginalMessage() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        String msg = "foo";
        register(packetHandlers, msg);

        Object outbound = writeOutbound(packetHandlers, msg);

        assertSame(msg, outbound);
    }

    @Test
    void handlerReturningEqualNewReferenceForwardsNewReference() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        String msg = "foo";
        String replacement = new String("foo");
        register(packetHandlers, replacement);

        Object outbound = writeOutbound(packetHandlers, msg);

        assertEquals(msg, outbound);
        assertNotSame(msg, outbound);
        assertSame(replacement, outbound);
    }

    @Test
    void multipleHandlersForwardLastNewReference() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        String msg = "foo";
        String firstReplacement = new String("first");
        String secondReplacement = new String("second");
        register(packetHandlers, firstReplacement);
        register(packetHandlers, secondReplacement);

        Object outbound = writeOutbound(packetHandlers, msg);

        assertSame(secondReplacement, outbound);
    }

    private static void register(PacketHandlersImpl packetHandlers, Object result) {
        PacketHandler owner = (ctx, packet, serverbound) -> packet;
        packetHandlers.register(owner, String.class, (ctx, packet, serverbound) -> result);
    }

    private static Object writeOutbound(PacketHandlersImpl packetHandlers, Object msg) {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutPacketHandler(packetHandlers, true));
        try {
            assertTrue(channel.writeOutbound(msg));
            Object outbound = channel.readOutbound();
            assertNull(channel.readOutbound());
            return outbound;
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
