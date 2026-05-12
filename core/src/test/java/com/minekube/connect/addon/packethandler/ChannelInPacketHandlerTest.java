package com.minekube.connect.addon.packethandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.minekube.connect.api.packet.PacketHandler;
import com.minekube.connect.packet.PacketHandlersImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

class ChannelInPacketHandlerTest {

    @Test
    void passesOriginalMessageWhenNoHandlersRegistered() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        EmbeddedChannel channel = newChannel(packetHandlers);
        String msg = new String("foo");

        channel.writeInbound(msg);
        Object forwarded = channel.readInbound();

        assertSame(msg, forwarded);
    }

    @Test
    void passesOriginalMessageWhenHandlerReturnsSameReference() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler owner = mock(PacketHandler.class);
        packetHandlers.register(owner, String.class, (ctx, packet, serverbound) -> packet);
        EmbeddedChannel channel = newChannel(packetHandlers);
        String msg = new String("foo");

        channel.writeInbound(msg);
        Object forwarded = channel.readInbound();

        assertSame(msg, forwarded);
    }

    @Test
    void forwardsNewReferenceEvenWhenItEqualsOriginalMessage() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler owner = mock(PacketHandler.class);
        packetHandlers.register(owner, String.class, (ctx, packet, serverbound) -> new String((String) packet));
        EmbeddedChannel channel = newChannel(packetHandlers);
        String msg = new String("foo");

        channel.writeInbound(msg);
        Object forwarded = channel.readInbound();

        assertEquals(msg, forwarded);
        assertNotSame(msg, forwarded);
    }

    @Test
    void forwardsLastReplacementWhenMultipleHandlersReturnNewReferences() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler firstOwner = mock(PacketHandler.class);
        PacketHandler secondOwner = mock(PacketHandler.class);
        String firstReplacement = new String("first");
        String secondReplacement = new String("second");
        packetHandlers.register(firstOwner, String.class, (ctx, packet, serverbound) -> firstReplacement);
        packetHandlers.register(secondOwner, String.class, (ctx, packet, serverbound) -> secondReplacement);
        EmbeddedChannel channel = newChannel(packetHandlers);

        channel.writeInbound(new String("foo"));
        Object forwarded = channel.readInbound();

        assertSame(secondReplacement, forwarded);
    }

    private static EmbeddedChannel newChannel(PacketHandlersImpl packetHandlers) {
        return new EmbeddedChannel(new ChannelInPacketHandler(packetHandlers, true));
    }
}
