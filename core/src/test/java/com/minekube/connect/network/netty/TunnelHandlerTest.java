package com.minekube.connect.network.netty;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.minekube.connect.api.logger.ConnectLogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TunnelHandlerTest {
    private DefaultEventLoop eventLoop;
    private Channel channel;
    private ChannelFuture closeFuture;
    private final List<RecordedEvent> events = new ArrayList<>();

    @AfterEach
    void shutdownEventLoop() throws Exception {
        if (eventLoop != null) {
            // Zero quiet period — tests don't need Netty's default 2s grace.
            eventLoop.shutdownGracefully(0, 5, SECONDS).get(5, SECONDS);
        }
    }

    @Test
    void onReceiveWritesPayloadAndFlushesOnce() throws Exception {
        TunnelHandler handler = newHandler();
        byte[] payload = new byte[] {1, 2, 3};

        runWithEventLoopBlocked(() -> handler.onReceive(payload));
        awaitEventLoop();

        assertEventTypes(Event.WRITE, Event.FLUSH);
        assertArrayEquals(payload, events.get(0).payload);
    }

    @Test
    void burstOfReceivesCoalescesIntoOneFlush() throws Exception {
        TunnelHandler handler = newHandler();
        List<byte[]> payloads = new ArrayList<>();

        runWithEventLoopBlocked(() -> {
            for (int i = 0; i < 50; i++) {
                byte[] payload = new byte[] {(byte) i, (byte) (i + 1)};
                payloads.add(payload);
                handler.onReceive(payload);
            }
        });
        awaitEventLoop();

        assertEquals(50, count(Event.WRITE));
        assertEquals(1, count(Event.FLUSH));
        List<byte[]> actualPayloads = writePayloads();
        assertEquals(50, actualPayloads.size());
        for (int i = 0; i < payloads.size(); i++) {
            assertArrayEquals(payloads.get(i), actualPayloads.get(i));
        }
    }

    @Test
    void onCloseFlushesPendingWriteBeforeClosingChannel() throws Exception {
        TunnelHandler handler = newHandler();
        byte[] payload = new byte[] {9, 8, 7};

        runWithEventLoopBlocked(() -> {
            handler.onReceive(payload);
            handler.onClose();
        });
        awaitEventLoop();

        // The CLOSE-driven flush must precede CLOSE so the payload reaches the
        // wire before the channel is torn down. A trailing FLUSH from the
        // deferred-write task is harmless (no-op on a closed channel in real Netty).
        List<Event> types = eventTypes();
        int firstFlush = types.indexOf(Event.FLUSH);
        int close = types.indexOf(Event.CLOSE);
        assertEquals(Event.WRITE, types.get(0));
        assertTrue(firstFlush > 0, "expected a FLUSH before CLOSE, got: " + types);
        assertTrue(close > firstFlush, "expected CLOSE after FLUSH, got: " + types);
        assertArrayEquals(payload, events.get(0).payload);
        assertTrue(closeFuture.isDone());
    }

    private TunnelHandler newHandler() {
        eventLoop = new DefaultEventLoop();
        channel = mock(Channel.class);
        closeFuture = mock(ChannelFuture.class);

        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.voidPromise()).thenReturn(mock(ChannelPromise.class));
        when(closeFuture.isDone()).thenReturn(true);

        doAnswer(invocation -> {
            ByteBuf buf = invocation.getArgument(0);
            try {
                byte[] payload = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), payload);
                events.add(new RecordedEvent(Event.WRITE, payload));
                return invocation.getArgument(1);
            } finally {
                buf.release();
            }
        }).when(channel).write(any(ByteBuf.class), any(ChannelPromise.class));

        doAnswer(invocation -> {
            events.add(new RecordedEvent(Event.FLUSH));
            return channel;
        }).when(channel).flush();

        doAnswer(invocation -> {
            events.add(new RecordedEvent(Event.CLOSE));
            return closeFuture;
        }).when(channel).close();

        return new TunnelHandler(mock(ConnectLogger.class), channel);
    }

    private void awaitEventLoop() throws Exception {
        eventLoop.submit(() -> null).get(5, SECONDS);
        eventLoop.submit(() -> null).get(5, SECONDS);
    }

    private void runWithEventLoopBlocked(Runnable task) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        eventLoop.execute(() -> {
            started.countDown();
            try {
                assertTrue(release.await(5, SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
        assertTrue(started.await(5, SECONDS));
        try {
            task.run();
        } finally {
            release.countDown();
        }
    }

    private void assertEventTypes(Event... expected) {
        assertEquals(Arrays.asList(expected), eventTypes());
    }

    private List<Event> eventTypes() {
        List<Event> types = new ArrayList<>();
        for (RecordedEvent event : events) {
            types.add(event.type);
        }
        return types;
    }

    private List<byte[]> writePayloads() {
        List<byte[]> payloads = new ArrayList<>();
        for (RecordedEvent event : events) {
            if (event.type == Event.WRITE) {
                payloads.add(event.payload);
            }
        }
        return payloads;
    }

    private int count(Event type) {
        int count = 0;
        for (RecordedEvent event : events) {
            if (event.type == type) {
                count++;
            }
        }
        return count;
    }

    private enum Event { WRITE, FLUSH, CLOSE }

    private static final class RecordedEvent {
        private final Event type;
        private final byte[] payload;

        private RecordedEvent(Event type) {
            this(type, null);
        }

        private RecordedEvent(Event type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }
    }
}
