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

package com.minekube.connect.tunnel.p2p;

import com.minekube.connect.tunnel.TunnelClientTransport;
import com.minekube.connect.tunnel.TunnelConn;
import io.libp2p.core.Stream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Objects;
import java.util.function.Consumer;
import minekube.connect.v1alpha1.WatchServiceOuterClass.TunnelTransport.Type;

final class SameStreamTunnelTransport implements TunnelClientTransport {
    private final Stream stream;
    private final Consumer<String> onOpened;

    SameStreamTunnelTransport(Stream stream, Consumer<String> onOpened) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.onOpened = Objects.requireNonNull(onOpened, "onOpened");
    }

    @Override
    public Type type() {
        return Type.TYPE_LIBP2P;
    }

    @Override
    public TunnelConn tunnel(String ignoredAddress, String sessionId, TunnelConn.Handler handler) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(handler, "handler");
        stream.pushHandler(new InboundBytesHandler(handler));
        onOpened.accept(sessionId);
        return new SameStreamTunnelConn(stream);
    }

    private static final class SameStreamTunnelConn extends TunnelConn {
        private final Stream stream;

        private SameStreamTunnelConn(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void write(byte[] data) {
            stream.writeAndFlush(Unpooled.wrappedBuffer(data));
        }

        @Override
        public void close(Throwable t) {
            stream.close();
        }

        @Override
        public boolean opened() {
            return true;
        }
    }

    private static final class InboundBytesHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final TunnelConn.Handler handler;

        private InboundBytesHandler(TunnelConn.Handler handler) {
            this.handler = handler;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            handler.onReceive(ByteBufUtil.getBytes(msg, msg.readerIndex(), msg.readableBytes(), false));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            handler.onError(cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            handler.onClose();
            super.channelInactive(ctx);
        }
    }
}
