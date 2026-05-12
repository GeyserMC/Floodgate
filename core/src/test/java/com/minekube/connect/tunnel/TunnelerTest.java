package com.minekube.connect.tunnel;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TunnelerTest {

    @Test
    void receivesBinaryFrameFromTunnelService() throws Exception {
        byte[] sent = new byte[] {1, 2, 3, 4, 5};

        byte[] received = receiveServerMessage(sent);

        assertArrayEquals(sent, received);
    }

    @Test
    void receivesBinaryFrameWhenByteStringDataFieldIsUnavailable() throws Exception {
        byte[] sent = new byte[] {9, 8, 7, 6};
        Field dataField = Tunneler.class.getDeclaredField("DATA");
        dataField.setAccessible(true);
        Field originalData = (Field) dataField.get(null);

        try {
            setStaticField(dataField, null);

            byte[] received = receiveServerMessage(sent);

            assertArrayEquals(sent, received);
        } finally {
            setStaticField(dataField, originalData);
        }
    }

    private static byte[] receiveServerMessage(byte[] sent) throws Exception {
        OkHttpClient client = new OkHttpClient();
        CapturingHandler handler = new CapturingHandler();
        TunnelConn conn = null;

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                    webSocket.send(ByteString.of(sent));
                }
            }));
            server.start();

            conn = new Tunneler(client).tunnel(webSocketUrl(server), "test-session", handler);

            return handler.awaitReceived();
        } finally {
            if (conn != null) {
                conn.close();
            }
            client.dispatcher().cancelAll();
            client.dispatcher().executorService().shutdownNow();
            client.connectionPool().evictAll();
        }
    }

    private static String webSocketUrl(MockWebServer server) {
        return server.url("/tunnel").toString().replaceFirst("^http:", "ws:");
    }

    private static void setStaticField(Field field, Object value) throws Exception {
        try {
            field.set(null, value);
            return;
        } catch (IllegalAccessException ignored) {
        }

        Object unsafe = unsafe();
        Method staticFieldBase = unsafe.getClass().getMethod("staticFieldBase", Field.class);
        Method staticFieldOffset = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
        Method putObject = unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class);
        Object base = staticFieldBase.invoke(unsafe, field);
        long offset = (Long) staticFieldOffset.invoke(unsafe, field);
        putObject.invoke(unsafe, base, offset, value);
    }

    private static Object unsafe() throws Exception {
        Field unsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        return unsafe.get(null);
    }

    private static final class CapturingHandler implements TunnelConn.Handler {
        private final AtomicReference<byte[]> received = new AtomicReference<>();
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onReceive(byte[] data) {
            received.set(data);
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
        }

        private byte[] awaitReceived() {
            await().atMost(5, SECONDS).untilAsserted(() -> {
                Throwable thrown = error.get();
                if (thrown != null) {
                    fail("Tunnel handler received an error", thrown);
                }
                assertNotNull(received.get());
            });
            return received.get();
        }
    }
}
