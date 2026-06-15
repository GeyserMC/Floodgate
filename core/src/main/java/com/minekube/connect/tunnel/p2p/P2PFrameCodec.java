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

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class P2PFrameCodec {
    public static final int MAX_CONTROL_FRAME_SIZE = 1 << 20;

    private P2PFrameCodec() {
    }

    public static void write(OutputStream out, MessageLite message) throws IOException {
        byte[] payload = message.toByteArray();
        if (payload.length > MAX_CONTROL_FRAME_SIZE) {
            throw new IllegalArgumentException("frame size " + payload.length
                    + " exceeds max " + MAX_CONTROL_FRAME_SIZE);
        }
        writeVarint(out, payload.length);
        out.write(payload);
        out.flush();
    }

    public static <T extends MessageLite> T read(
            InputStream in,
            Parser<T> parser,
            int maxFrameSize
    ) throws IOException {
        int max = maxFrameSize <= 0 ? MAX_CONTROL_FRAME_SIZE : maxFrameSize;
        long size = readVarint(in);
        if (size > max) {
            throw new IllegalArgumentException("frame size " + size + " exceeds max " + max);
        }
        byte[] payload = in.readNBytes((int) size);
        if (payload.length != size) {
            throw new EOFException("truncated frame payload");
        }
        return parser.parseFrom(payload);
    }

    private static void writeVarint(OutputStream out, int value) throws IOException {
        long v = value & 0xffffffffL;
        while (v >= 0x80) {
            out.write((int) v | 0x80);
            v >>>= 7;
        }
        out.write((int) v);
    }

    private static long readVarint(InputStream in) throws IOException {
        long value = 0;
        int shift = 0;
        for (int i = 0; i < 10; i++) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException("truncated frame length");
            }
            if (b < 0x80) {
                if (i == 9 && b > 1) {
                    throw new IllegalArgumentException("varint overflows a 64-bit integer");
                }
                return value | ((long) b << shift);
            }
            value |= (long) (b & 0x7f) << shift;
            shift += 7;
        }
        throw new IllegalArgumentException("varint overflows a 64-bit integer");
    }
}
