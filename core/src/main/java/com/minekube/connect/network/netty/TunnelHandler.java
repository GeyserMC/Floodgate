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

package com.minekube.connect.network.netty;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.tunnel.TunnelConn.Handler;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TunnelHandler implements Handler {
    private final ConnectLogger logger;
    private final Channel downstreamServerConn; // local server connection

    // Coalesces flushes across an EventLoop tick: one flush() per batch of
    // onReceive calls instead of one per packet. The CAS lives inside the
    // write task so the flush is always enqueued after the write that needs
    // it — scheduling the CAS outside the EventLoop races, because a later
    // write can be enqueued behind an already-scheduled flush.
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    @Override
    public void onReceive(byte[] data) {
        // TunnelService -> local session server -> downstream server.
        // Allocate the ByteBuf inside the lambda so it isn't leaked if execute()
        // rejects (event loop shutting down during proxy stop).
        Channel ch = downstreamServerConn;
        EventLoop el = ch.eventLoop();
        try {
            el.execute(() -> {
                ch.write(Unpooled.wrappedBuffer(data), ch.voidPromise());
                if (flushScheduled.compareAndSet(false, true)) {
                    try {
                        el.execute(() -> {
                            flushScheduled.set(false);
                            ch.flush();
                        });
                    } catch (RejectedExecutionException ignored) {
                        flushScheduled.set(false);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Event loop is shutting down; the channel is going away anyway.
        }
    }

    @Override
    public void onError(Throwable t) {
        // error connecting to tunnel service
        Status status = Status.fromThrowable(t);
        if (status.getCode() == Code.CANCELLED) {
            return;
        }
        logger.error("Connection error with TunnelService: " +
                        t + (
                        t.getCause() == null ? ""
                                : " (cause: " + t.getCause().toString() + ")"
                )
        );
    }

    @Override
    public void onClose() {
        // Flush before closing: deferred writes from onReceive() may still be
        // sitting in the channel's outbound buffer with the flush scheduled as
        // a separate EventLoop task, so closing without a final flush can drop
        // the last payload.
        Channel ch = downstreamServerConn;
        try {
            ch.eventLoop().execute(() -> {
                ch.flush();
                ch.close();
            });
        } catch (RejectedExecutionException ignored) {
            // Event loop already shut down: close directly. Netty's close is
            // thread-safe and a no-op on an already-closed channel.
            ch.close();
        }
    }
}
