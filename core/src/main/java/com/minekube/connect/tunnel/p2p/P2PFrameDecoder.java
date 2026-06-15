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
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

final class P2PFrameDecoder<T extends MessageLite> extends ByteToMessageDecoder {
    private final Parser<T> parser;
    private final int maxFrameSize;

    P2PFrameDecoder(Parser<T> parser, int maxFrameSize) {
        this.parser = parser;
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();
        int length = 0;
        int shift = 0;
        for (int i = 0; i < 5; i++) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }
            int b = in.readUnsignedByte();
            length |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                if (length < 0 || length > maxFrameSize) {
                    throw new IllegalArgumentException("protobuf frame length " + length
                            + " exceeds max " + maxFrameSize);
                }
                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return;
                }
                byte[] bytes = new byte[length];
                in.readBytes(bytes);
                out.add(parser.parseFrom(bytes));
                return;
            }
            shift += 7;
        }
        throw new IllegalArgumentException("protobuf frame length varint overflow");
    }
}
