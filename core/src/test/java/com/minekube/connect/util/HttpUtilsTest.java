package com.minekube.connect.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HttpUtilsTest {
    private static final String THREAD_PREFIX = "connect-http-worker";

    @Test
    void executorRunsAtLeastFourTasksConcurrently() throws Exception {
        ExecutorService executorService = executorService();
        CountDownLatch started = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        Future<?>[] futures = new Future<?>[4];

        try {
            for (int i = 0; i < futures.length; i++) {
                futures[i] = executorService.submit(() -> {
                    int current = running.incrementAndGet();
                    peak.accumulateAndGet(current, Math::max);
                    started.countDown();
                    try {
                        release.await(1, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        running.decrementAndGet();
                    }
                });
            }

            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(4, peak.get());
        } finally {
            release.countDown();
            for (Future<?> future : futures) {
                if (future != null) {
                    future.get(1, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Test
    void executorUsesConnectHttpWorkerThreadNames() throws Exception {
        Future<String> threadName = executorService().submit(() -> Thread.currentThread().getName());

        assertTrue(threadName.get(1, TimeUnit.SECONDS).startsWith(THREAD_PREFIX));
    }

    @Test
    void executorUsesDaemonThreads() throws Exception {
        Future<Boolean> daemon = executorService().submit(() -> Thread.currentThread().isDaemon());

        assertTrue(daemon.get(1, TimeUnit.SECONDS));
    }

    private static ExecutorService executorService() throws Exception {
        Field field = HttpUtils.class.getDeclaredField("EXECUTOR_SERVICE");
        field.setAccessible(true);
        return (ExecutorService) field.get(null);
    }
}
