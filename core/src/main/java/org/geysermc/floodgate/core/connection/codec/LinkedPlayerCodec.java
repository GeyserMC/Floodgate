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

import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readString;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readUniqueId;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeString;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeUniqueId;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.geysermc.floodgate.util.LinkedPlayer;

public final class LinkedPlayerCodec {
    private LinkedPlayerCodec() {}

    public static void encode(LinkedPlayer linkedPlayer, DataOutputStream stream) throws IOException {
        writeString(stream, linkedPlayer.getJavaUsername());
        writeUniqueId(stream, linkedPlayer.getJavaUniqueId());
        writeUniqueId(stream, linkedPlayer.getBedrockId());
    }

    public static LinkedPlayer decode(ByteBuffer buffer) {
        return LinkedPlayer.of(readString(buffer), readUniqueId(buffer), readUniqueId(buffer));
    }
}
