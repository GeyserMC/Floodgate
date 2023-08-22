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

package org.geysermc.floodgate.core.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;
import org.geysermc.floodgate.core.crypto.topping.Topping;
import org.geysermc.floodgate.core.util.InvalidFormatException;

@Singleton
public final class FloodgateDataCodec {
    public static final int VERSION = 2;
    public static final byte[] IDENTIFIER = "^Floodgate^".getBytes(UTF_8);
    public static final byte[] HEADER = (new String(IDENTIFIER, UTF_8) + (char) (VERSION + 0x3D)).getBytes(UTF_8);

    private final DataCodec codec;

    private final Topping topping;

    public FloodgateDataCodec(
            DataCodecType type,
            Topping topping,
            @Named("dataDirectory") Path dataDirectory
    ) throws IOException {
        Objects.requireNonNull(type);
        this.codec = type.dataCodec();
        this.topping = topping;

        var keyCodecBase = type.keyCodec();
        if (type.asymmetrical()) {
            var keyPair = ((KeyCodecPair) keyCodecBase).decode(dataDirectory);
            ((DataCodecKeyPair) codec).init(keyPair);
        } else {
            var key = ((KeyCodecSingle) keyCodecBase).decode(dataDirectory);
            codec.init(key);
        }
    }

    public static int version(String data) {
        if (data.length() < HEADER.length) {
            return -1;
        }

        for (int i = 0; i < IDENTIFIER.length; i++) {
            if (IDENTIFIER[i] != data.charAt(i)) {
                return -1;
            }
        }

        return data.charAt(IDENTIFIER.length) - 0x3D;
    }

    public byte[] encode(ByteBuffer data) throws Exception {
        var encryptedSections = codec.encode(data);
        var encodedData = topping.encode(encryptedSections);

        return ByteBuffer.allocate(HEADER.length + encodedData.remaining())
                .put(HEADER)
                .put(encodedData)
                .array();
    }

    public byte[] encodeFromString(String data) throws Exception {
        return encode(ByteBuffer.wrap(data.getBytes(UTF_8)));
    }

    public ByteBuffer decode(byte[] data) throws Exception {
        checkHeader(data);

        int bufferLength = data.length - HEADER.length;
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER.length, bufferLength);

        var encryptedSections = topping.decode(buffer);
        return codec.decode(encryptedSections);
    }

    public String decodeToString(byte[] data) throws Exception {
        ByteBuffer decrypted = decode(data);

        byte[] decryptedBytes = new byte[decrypted.remaining()];
        decrypted.get(decryptedBytes);

        return new String(decryptedBytes, UTF_8);
    }

    public ByteBuffer decodeFromString(String data) throws Exception {
        return decode(data.getBytes(UTF_8));
    }

    /**
     * Checks if the header is valid
     *
     * @param data the data to check
     * @throws InvalidFormatException when the header is invalid
     */
    public void checkHeader(byte[] data) throws InvalidFormatException {
        if (data.length < HEADER.length) {
            throw new InvalidFormatException(
                    "Data length is smaller then header." +
                            "Needed " + HEADER.length + ", got " + data.length
            );
        }

        for (int i = 0; i < IDENTIFIER.length; i++) {
            if (IDENTIFIER[i] != data[i]) {
                String identifier = new String(IDENTIFIER, UTF_8);
                String received = new String(data, 0, IDENTIFIER.length, UTF_8);
                throw new InvalidFormatException(
                        "Expected identifier " + identifier + ", got " + received
                );
            }
        }
    }
}
