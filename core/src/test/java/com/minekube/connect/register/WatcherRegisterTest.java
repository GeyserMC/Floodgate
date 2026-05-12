package com.minekube.connect.register;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.inject.PlatformInjector;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.WatchClient;
import com.minekube.connect.watch.Watcher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WatcherRegisterTest {
    private WatcherRegister register;

    @AfterEach
    void stopRegister() {
        if (register != null) {
            register.stop();
        }
    }

    @Test
    void constructorDoesNotCreateScheduler() throws Exception {
        WatcherRegister register = new WatcherRegister();

        assertNull(scheduler(register));
    }

    @Test
    void startCreatesScheduler() throws Exception {
        register = newRegister();

        register.start();

        ScheduledExecutorService scheduler = scheduler(register);
        assertNotNull(scheduler);
        assertFalse(scheduler.isShutdown());
    }

    @Test
    void stopShutsDownSchedulerAndClearsField() throws Exception {
        register = newRegister();
        register.start();
        ScheduledExecutorService scheduler = scheduler(register);

        register.stop();

        assertNull(scheduler(register));
        assertTrue(scheduler.isShutdown());
    }

    @Test
    void startAfterStopCreatesNewScheduler() throws Exception {
        register = newRegister();
        register.start();
        ScheduledExecutorService firstScheduler = scheduler(register);

        register.stop();
        register.start();

        ScheduledExecutorService secondScheduler = scheduler(register);
        assertNotNull(secondScheduler);
        assertNotSame(firstScheduler, secondScheduler);
        assertTrue(firstScheduler.isShutdown());
        assertFalse(secondScheduler.isShutdown());
    }

    @Test
    void stopBeforeStartDoesNotCreateScheduler() throws Exception {
        WatcherRegister register = new WatcherRegister();

        assertDoesNotThrow(register::stop);

        assertNull(scheduler(register));
    }

    @Test
    void retryDoesNotThrowWhenSchedulerIsClearedAfterStop() throws Exception {
        register = newRegister();
        register.start();
        register.stop();

        // Force the race window: started=true but scheduler already cleared.
        started(register).set(true);

        assertDoesNotThrow(() -> invokeRetry(register));
        assertNull(scheduler(register));
    }

    @Test
    void resetBackOffTimerDoesNotThrowWhenSchedulerIsClearedAfterStop() throws Exception {
        Fixture fixture = newFixture();
        register = fixture.register;
        register.start();
        ArgumentCaptor<Watcher> watcher = ArgumentCaptor.forClass(Watcher.class);
        verify(fixture.watchClient).watch(watcher.capture());

        register.stop();

        assertDoesNotThrow(() -> watcher.getValue().onOpen());
        assertNull(scheduler(register));
    }

    @Test
    void watcherRegisterDoesNotDeclareTimerFields() {
        assertNoTimerFields(WatcherRegister.class);
        for (Class<?> nestedClass : WatcherRegister.class.getDeclaredClasses()) {
            assertNoTimerFields(nestedClass);
        }
    }

    private static WatcherRegister newRegister() throws Exception {
        return newFixture().register;
    }

    private static Fixture newFixture() throws Exception {
        WatcherRegister register = new WatcherRegister();
        WatchClient watchClient = mock(WatchClient.class);
        when(watchClient.watch(any(Watcher.class))).thenReturn(mock(WebSocket.class));

        inject(register, "watchClient", watchClient);
        inject(register, "tunneler", mock(Tunneler.class));
        inject(register, "platformInjector", mock(PlatformInjector.class));
        inject(register, "logger", mock(ConnectLogger.class));
        inject(register, "api", mock(SimpleConnectApi.class));
        return new Fixture(register, watchClient);
    }

    private static void inject(WatcherRegister register, String fieldName, Object value)
            throws Exception {
        Field field = WatcherRegister.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(register, value);
    }

    private static ScheduledExecutorService scheduler(WatcherRegister register) throws Exception {
        Field field = WatcherRegister.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(register);
    }

    private static AtomicBoolean started(WatcherRegister register) throws Exception {
        Field field = WatcherRegister.class.getDeclaredField("started");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(register);
    }

    private static void invokeRetry(WatcherRegister register) throws Exception {
        Method method = WatcherRegister.class.getDeclaredMethod("retry");
        method.setAccessible(true);
        method.invoke(register);
    }

    private static void assertNoTimerFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            assertFalse(Timer.class.isAssignableFrom(field.getType()),
                    type.getName() + "#" + field.getName() + " must not use java.util.Timer");
        }
    }

    private static final class Fixture {
        private final WatcherRegister register;
        private final WatchClient watchClient;

        private Fixture(WatcherRegister register, WatchClient watchClient) {
            this.register = register;
            this.watchClient = watchClient;
        }
    }
}
