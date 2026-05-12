package com.minekube.connect.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.minekube.connect.api.inject.InjectorAddon;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CommonPlatformInjectorTest {
    private static final int THREADS = 8;
    private static final int CHANNELS_PER_THREAD = 100;
    @Test
    void addInjectedClientReturnsFalseForDuplicateChannel() {
        TestInjector injector = new TestInjector();
        EmbeddedChannel channel = new EmbeddedChannel();
        assertTrue(injector.addClient(channel));
        assertFalse(injector.addClient(channel));
        assertEquals(1, injector.injectedClientCount());
    }
    @Test
    void removeInjectedClientRemovesChannel() {
        TestInjector injector = new TestInjector();
        EmbeddedChannel channel = new EmbeddedChannel();
        injector.addClient(channel);
        assertTrue(injector.removeClient(channel));
        assertFalse(injector.removeClient(channel));
        assertEquals(0, injector.injectedClientCount());
    }
    @Test
    void addInjectedClientAcceptsConcurrentAdds() throws Exception {
        TestInjector injector = new TestInjector();
        AtomicReference<Throwable> failure = runConcurrently(() -> {
            for (int i = 0; i < CHANNELS_PER_THREAD; i++) {
                injector.addClient(new EmbeddedChannel());
            }
        });
        assertNull(failure.get());
        assertEquals(THREADS * CHANNELS_PER_THREAD, injector.injectedClientCount());
    }
    @Test
    void injectedClientsCanBeSizedWhileConcurrentAddsRun() throws Exception {
        TestInjector injector = new TestInjector();
        AtomicBoolean adding = new AtomicBoolean(true);
        AtomicReference<Throwable> sizeFailure = new AtomicReference<>();
        Thread sizer = new Thread(() -> {
            try {
                while (adding.get()) {
                    injector.injectedClientCount();
                    Thread.yield();
                }
            } catch (Throwable throwable) {
                sizeFailure.compareAndSet(null, throwable);
            }
        });
        AtomicReference<Throwable> addFailure;
        try {
            sizer.start();
            addFailure = runConcurrently(() -> {
                for (int i = 0; i < CHANNELS_PER_THREAD; i++) {
                    injector.addClient(new EmbeddedChannel());
                }
            });
        } finally {
            adding.set(false);
            sizer.join(TimeUnit.SECONDS.toMillis(5));
        }
        assertFalse(sizer.isAlive());
        assertNull(addFailure.get());
        assertNull(sizeFailure.get());
        assertEquals(THREADS * CHANNELS_PER_THREAD, injector.injectedClientCount());
    }
    @Test
    void injectAddonsCallCanIterateWhileAddonsChange() throws Exception {
        TestInjector injector = new TestInjector();
        InjectorAddon addon = mock(InjectorAddon.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        AtomicBoolean mutating = new AtomicBoolean(true);
        AtomicReference<Throwable> iterationFailure = new AtomicReference<>();
        Thread iterator = new Thread(() -> {
            try {
                while (mutating.get()) {
                    injector.injectAddonsCall(channel, true);
                    Thread.yield();
                }
            } catch (Throwable throwable) {
                iterationFailure.compareAndSet(null, throwable);
            }
        });
        AtomicReference<Throwable> mutationFailure;
        try {
            iterator.start();
            mutationFailure = runConcurrently(() -> {
                // 200 mutations × 8 threads = 1600 add/remove pairs — enough to
                // provoke a CME if the data structures aren't thread-safe.
                for (int i = 0; i < 200; i++) {
                    injector.addAddon(addon);
                    injector.removeAddon(addon.getClass());
                }
            });
        } finally {
            mutating.set(false);
            iterator.join(TimeUnit.SECONDS.toMillis(5));
        }
        assertFalse(iterator.isAlive());
        assertNull(mutationFailure.get());
        assertNull(iterationFailure.get());
    }
    private static AtomicReference<Throwable> runConcurrently(CheckedRunnable task)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    }
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            return failure;
        } finally {
            executor.shutdownNow();
        }
    }
    private interface CheckedRunnable { void run() throws Exception; }
    private static final class TestInjector extends CommonPlatformInjector {
        boolean addClient(Channel channel) { return addInjectedClient(channel); }
        boolean removeClient(Channel channel) { return removeInjectedClient(channel); }
        int injectedClientCount() { return getInjectedClients().size(); }
        @Override
        public boolean inject() { return true; }
        @Override
        public boolean isInjected() { return true; }
    }
}
