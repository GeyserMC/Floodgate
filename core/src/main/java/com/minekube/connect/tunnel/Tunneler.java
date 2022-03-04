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

package com.minekube.connect.tunnel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.minekube.connect.tunnel.TunnelConn.Handler;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Tunneler implements Closeable {

    private final OkHttpClient httpClient;

    @Inject
    public Tunneler(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private static final String SESSION_HEADER = "Connect-Session";

    public TunnelConn tunnel(final String tunnelServiceAddr, String sessionId, Handler handler) {
        checkNotNull(tunnelServiceAddr, "tunnelServiceAddr must not be null");
        checkNotNull(sessionId, "sessionId must not be null");
        checkNotNull(handler, "handler must not be null");
        checkArgument(!tunnelServiceAddr.isEmpty(),
                "tunnelServiceAddr must not be empty");
        checkArgument(!sessionId.isEmpty(), "sessionId must not be empty");

        Request request = new Request.Builder()
                .url(tunnelServiceAddr) // TODO default env var
                .addHeader(SESSION_HEADER, sessionId)
                .build();

        AtomicBoolean closeHandlerOnce = new AtomicBoolean();
        Runnable handlerOnClose = () -> {
            if (closeHandlerOnce.compareAndSet(false, true)) {
                handler.onClose();
            }
        };

        WebSocket ws = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                handlerOnClose.run();
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t,
                                  @Nullable Response response) {
                handler.onError(t);
                handlerOnClose.run();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                handler.onReceive(bytes.toByteArray());
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                // TODO log connected(?)
            }
        });

        return new TunnelConn() {
            @Override
            public void write(byte[] data) {
                ws.send(ByteString.of(data));
            }

            @Override
            public void close(Throwable t) {
                if (t == null) {
                    ws.close(1000, "tunnel closed clientside");
                } else {
                    ws.close(1002, t.getLocalizedMessage());
                }
                handlerOnClose.run();
            }
        };
    }

    @Override
    public void close() { // todo call on plugin shutdown
        // closes all requests to tunnel services,
        // disconnects tunneled players instantaneously
        Stream.of(httpClient.dispatcher().runningCalls(), httpClient.dispatcher().queuedCalls())
                .flatMap(Collection::stream)
                .filter(call -> call.request().header(SESSION_HEADER) != null)
                .forEach(Call::cancel);
    }
}
