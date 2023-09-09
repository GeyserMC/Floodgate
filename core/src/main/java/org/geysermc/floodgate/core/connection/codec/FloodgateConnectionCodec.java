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

import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readBool;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readIp;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readString;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readUniqueId;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.readUnsignedLong;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeIp;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeString;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeUniqueId;
import static org.geysermc.floodgate.core.connection.codec.CodecUtils.writeUnsignedLong;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.api.util.UiProfile;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.connection.FloodgateConnectionBuilder;

@Singleton
public final class FloodgateConnectionCodec {
    @Inject FloodgateConfig config;

    public ByteBuffer encode(FloodgateConnection connection) {
        var byteStream = new ByteArrayOutputStream();
        var stream = new DataOutputStream(byteStream);
        try {
            encode0(connection, stream);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return ByteBuffer.wrap(byteStream.toByteArray());
    }

    private void encode0(FloodgateConnection connection, DataOutputStream stream) throws IOException {
        writeString(stream, connection.version());
        writeString(stream, connection.bedrockUsername());
        writeUniqueId(stream, connection.identifier());
        writeUnsignedLong(stream, connection.xuid());
        stream.writeByte(connection.platform().ordinal());
        writeString(stream, connection.languageCode());
        stream.writeByte(connection.uiProfile().ordinal());
        stream.writeByte(connection.inputMode().ordinal());
        writeIp(stream, connection.ip());

        stream.writeBoolean(connection.isLinked());
        if (connection.linkedPlayer() != null) {
            LinkedPlayerCodec.encode(connection.linkedPlayer(), stream);
        }
    }

    public FloodgateConnection decode(ByteBuffer buffer) {
        var builder = new FloodgateConnectionBuilder(config)
                .version(readString(buffer))
                .username(readString(buffer))
                .identifier(readUniqueId(buffer))
                .xuid(readUnsignedLong(buffer))
                .deviceOs(BedrockPlatform.fromId(buffer.get()))
                .languageCode(readString(buffer))
                .uiProfile(UiProfile.fromId(buffer.get()))
                .inputMode(InputMode.fromId(buffer.get()))
                .ip(readIp(buffer));
        if (readBool(buffer)) {
            builder.linkedPlayer(LinkedPlayerCodec.decode(buffer));
        }

        if (buffer.hasRemaining()) {
            throw new IllegalStateException("There are still bytes (" + buffer.remaining() + ") left!");
        }

        return builder.build();
    }
}
