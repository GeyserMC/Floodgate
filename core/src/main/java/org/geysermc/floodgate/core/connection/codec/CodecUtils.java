/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.core.connection.codec;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class CodecUtils {
    private CodecUtils() {}

    public static boolean readBool(ByteBuffer buffer) {
        return buffer.get() == 1;
    }

    public static String readString(ByteBuffer buffer) {
        var bytes = new byte[readVarInt(buffer)];
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(DataOutputStream stream, String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(stream, bytes.length);
        stream.write(bytes);
    }

    public static int readVarInt(ByteBuffer buffer) {
        int value = 0;
        int size = 0;
        int b;
        while (((b = buffer.get()) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) {
                throw new IllegalArgumentException("VarInt too long (length must be <= 5)");
            }
        }

        return value | ((b & 0x7F) << (size * 7));
    }

    public static void writeVarInt(DataOutputStream stream, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            stream.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }

        stream.writeByte(value);
    }
    
    public static UUID readUniqueId(ByteBuffer buffer) {
        return new UUID(buffer.getLong(), buffer.getLong());
    }
    
    public static void writeUniqueId(DataOutputStream stream, UUID value) throws IOException {
        stream.writeLong(value.getMostSignificantBits());
        stream.writeLong(value.getLeastSignificantBits());
    }
}
