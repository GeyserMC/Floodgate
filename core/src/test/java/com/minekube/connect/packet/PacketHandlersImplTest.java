package com.minekube.connect.packet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.minekube.connect.api.packet.PacketHandler;
import com.minekube.connect.api.util.TriFunction;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PacketHandlersImplTest {
    private static final TriFunction<ChannelHandlerContext, Object, Boolean, Object> STRING_CONSUMER =
            (ctx, packet, serverbound) -> "string";
    private static final TriFunction<ChannelHandlerContext, Object, Boolean, Object> INTEGER_CONSUMER =
            (ctx, packet, serverbound) -> "integer";

    @Test
    void registerForPacketClassReturnsConsumerOnlyForThatClass() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler owner = mock(PacketHandler.class);

        packetHandlers.register(owner, String.class, STRING_CONSUMER);

        assertTrue(packetHandlers.getPacketHandlers(String.class).contains(STRING_CONSUMER));
        assertTrue(packetHandlers.getPacketHandlers(Integer.class).isEmpty());
    }

    @Test
    void registerAllAddsGlobalHandlerToExistingPacketClasses() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler owner = mock(PacketHandler.class);
        PacketHandler globalHandler = (ctx, packet, serverbound) -> "global";
        packetHandlers.register(owner, String.class, STRING_CONSUMER);

        packetHandlers.registerAll(globalHandler);

        Collection<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> handlers =
                packetHandlers.getPacketHandlers(String.class);
        assertTrue(handlers.contains(STRING_CONSUMER));
        assertTrue(anyHandlerReturns(handlers, "global"));
    }

    @Test
    void deregisterRemovesPacketClassHandlersCleanly() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler owner = mock(PacketHandler.class);
        packetHandlers.register(owner, String.class, STRING_CONSUMER);

        packetHandlers.deregister(owner);

        assertTrue(packetHandlers.getPacketHandlers(String.class).isEmpty());
        assertFalse(packetHandlers.hasHandlers());
    }

    @Test
    void deregisterGlobalHandlerDoesNotThrowAndRemovesItFromFuturePacketClasses() {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        PacketHandler globalHandler = (ctx, packet, serverbound) -> "global";
        PacketHandler owner = mock(PacketHandler.class);
        packetHandlers.registerAll(globalHandler);

        assertDoesNotThrow(() -> packetHandlers.deregister(globalHandler));

        packetHandlers.register(owner, String.class, STRING_CONSUMER);

        Collection<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> handlers =
                packetHandlers.getPacketHandlers(String.class);
        assertTrue(handlers.contains(STRING_CONSUMER));
        assertFalse(anyHandlerReturns(handlers, "global"));
    }

    @Test
    void concurrentRegisterAndDeregisterDoesNotLeaveRegisteredHandlers() throws Exception {
        PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
        int threads = 8;
        int iterations = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < threads; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        PacketHandler owner = newOwner();
                        TriFunction<ChannelHandlerContext, Object, Boolean, Object> consumer =
                                (ctx, packet, serverbound) -> packet;
                        packetHandlers.register(owner, String.class, consumer);
                        packetHandlers.deregister(owner);
                    }
                    return null;
                }));
            }

            start.countDown();

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertFalse(packetHandlers.hasHandlers());
    }

    @Test
    void concurrentRegisterDuringDeregisterKeepsNewlyRegisteredHandler() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 30 iterations catches the same races without paying Mockito mock
            // construction cost 100×.
            for (int iteration = 0; iteration < 30; iteration++) {
                PacketHandlersImpl packetHandlers = new PacketHandlersImpl();
                PacketHandler handlerA = mock(PacketHandler.class);
                PacketHandler handlerB = mock(PacketHandler.class);
                CountDownLatch start = new CountDownLatch(1);

                Future<?> registerA = executor.submit(() -> {
                    start.await();
                    packetHandlers.register(handlerA, String.class, STRING_CONSUMER);
                    return null;
                });
                Future<?> registerBThenDeregisterA = executor.submit(() -> {
                    start.await();
                    packetHandlers.register(handlerB, String.class, INTEGER_CONSUMER);
                    packetHandlers.deregister(handlerA);
                    return null;
                });

                start.countDown();
                registerA.get(5, TimeUnit.SECONDS);
                registerBThenDeregisterA.get(5, TimeUnit.SECONDS);

                assertTrue(
                        packetHandlers.getPacketHandlers(String.class).contains(INTEGER_CONSUMER),
                        "consumer registered during deregister was dropped on iteration " + iteration);
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static PacketHandler newOwner() {
        return new PacketHandler() {
            @Override
            public Object handle(ChannelHandlerContext ctx, Object packet, boolean serverbound) {
                return packet;
            }
        };
    }

    private static boolean anyHandlerReturns(
            Collection<TriFunction<ChannelHandlerContext, Object, Boolean, Object>> handlers,
            Object expected) {
        for (TriFunction<ChannelHandlerContext, Object, Boolean, Object> handler : handlers) {
            if (expected.equals(handler.apply(null, "packet", true))) {
                return true;
            }
        }
        return false;
    }
}
