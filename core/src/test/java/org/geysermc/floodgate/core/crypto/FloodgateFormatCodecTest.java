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

import static org.geysermc.floodgate.core.crypto.FloodgateFormatCodec.VERSION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.geysermc.floodgate.core.crypto.exception.UnsupportedVersionException;
import org.geysermc.floodgate.core.crypto.topping.Base64Topping;
import org.geysermc.floodgate.core.util.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

final class FloodgateFormatCodecTest {
    @ParameterizedTest
    @CsvSource({
            "^Floodgate^, -1",
            "^Flootgate^=, -1",
            "^Floodgate^=, 0",
            "^Floodgate^>, 1",
            "^Floodgate^A, 4",
    })
    void version(ArgumentsAccessor arguments) {
        assertEquals(arguments.getInteger(1), FloodgateFormatCodec.version(arguments.getString(0)));
    }

    @Test
    void createWithAsymmetricalKey() {
        assertDoesNotThrow(this::createFloodgateDataCodec);
    }

    @Test
    void createWithSymmetricalKey() {
        assertDoesNotThrow(() -> new FloodgateFormatCodec(
                DataCodecType.AES,
                new Base64Topping(),
                Path.of("src/test/resources/crypto")
        ));
    }

    @ParameterizedTest
    @CsvSource({
            "Hello, ^Floodgate^?SGVsbG8=!A03EErWCHgcPvI1HW7uSdmXFcfLr3ioAknyIKMsF7YFOfF5IDa_GBiWebaBIASVvokd2JsLp8lKQH3VNhJ0gAw==",
            "Hello world, ^Floodgate^?SGVsbG8gd29ybGQ=!U5q3m-0vreTlUj7EgmN7ipwR0ma5tEOBNQIVrWO5YsatKyZgT6V4rIAr4cmieThBfrMTJoPTJfePCFUtdgVnBA==",
            "\0\11\22\44\23, ^Floodgate^?JA==!uiU2PdBXPHRuJTJ6qCNOtn0eCvxddlL_2yOnxaw-9c7NRt7VGex6uUBwU9PAe4QpFBCVNAEDdHlrAIO_3vDgAg=="
    })
    void encodeFromString(ArgumentsAccessor arguments) throws Exception {
        var payload = arguments.getString(0);
        var encodedExpected = arguments.getString(1).getBytes(StandardCharsets.UTF_8);

        var codec = createFloodgateDataCodec();
        var encoded = codec.encodeFromString(payload);
        assertArrayEquals(encodedExpected, encoded);
    }

    @ParameterizedTest
    @CsvSource({
            "Hello, ^Floodgate^?SGVsbG8=!A03EErWCHgcPvI1HW7uSdmXFcfLr3ioAknyIKMsF7YFOfF5IDa_GBiWebaBIASVvokd2JsLp8lKQH3VNhJ0gAw==",
            "Hello world, ^Floodgate^?SGVsbG8gd29ybGQ=!U5q3m-0vreTlUj7EgmN7ipwR0ma5tEOBNQIVrWO5YsatKyZgT6V4rIAr4cmieThBfrMTJoPTJfePCFUtdgVnBA==",
            "\0\11\22\44\23, ^Floodgate^?JA==!uiU2PdBXPHRuJTJ6qCNOtn0eCvxddlL_2yOnxaw-9c7NRt7VGex6uUBwU9PAe4QpFBCVNAEDdHlrAIO_3vDgAg=="
    })
    void decodeFromString(ArgumentsAccessor arguments) throws Exception {
        var payloadExpected = ByteBuffer.wrap(arguments.getString(0).getBytes(StandardCharsets.UTF_8));
        var encoded = arguments.getString(1);

        var codec = createFloodgateDataCodec();
        var payload = codec.decodeFromString(encoded);
        assertEquals(0, payloadExpected.compareTo(payload));
    }

    @ParameterizedTest
    @CsvSource({
            "Hello, ^Floodgate^?SGVsbG8=!A03EErWCHgcPvI1HW7uSdmXFcfLr3ioAknyIKMsF7YFOfF5IDa_GBiWebaBIASVvokd2JsLp8lKQH3VNhJ0gAw==",
            "Hello world, ^Floodgate^?SGVsbG8gd29ybGQ=!U5q3m-0vreTlUj7EgmN7ipwR0ma5tEOBNQIVrWO5YsatKyZgT6V4rIAr4cmieThBfrMTJoPTJfePCFUtdgVnBA==",
            "\0\11\22\44\23, ^Floodgate^?JA==!uiU2PdBXPHRuJTJ6qCNOtn0eCvxddlL_2yOnxaw-9c7NRt7VGex6uUBwU9PAe4QpFBCVNAEDdHlrAIO_3vDgAg=="
    })
    void decodeToString(ArgumentsAccessor arguments) throws Exception {
        var payloadExpected = arguments.getString(0);
        var encoded = arguments.getString(1).getBytes(StandardCharsets.UTF_8);

        var codec = createFloodgateDataCodec();
        var payload = codec.decodeToString(encoded);
        assertEquals(payloadExpected, payload);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "^Floodgate^",
            "^Flootgate^="
    })
    void headerInvalidFormat(String content) throws IOException {
        var codec = createFloodgateDataCodec();

        assertThrowsExactly(
                InvalidFormatException.class,
                () -> codec.validateHeader(content.getBytes(StandardCharsets.UTF_8))
        );
    }

    @ParameterizedTest
    @CsvSource({
            (VERSION - 1) + ", false",
            VERSION + ", true",
            (VERSION + 1) + ", false"
    })
    void headerVersionValidation(ArgumentsAccessor arguments) throws IOException {
        var codec = createFloodgateDataCodec();

        var version = arguments.getInteger(0);
        var content = ("^Floodgate^" + (char) (version + 0x3D)).getBytes(StandardCharsets.UTF_8);

        var valid = arguments.getBoolean(1);

        if (valid) {
            assertDoesNotThrow(() -> codec.validateHeader(content));
        } else {
            assertThrowsExactly(UnsupportedVersionException.class, () -> codec.validateHeader(content));
        }
    }

    private FloodgateFormatCodec createFloodgateDataCodec() throws IOException {
        return new FloodgateFormatCodec(DataCodecType.ED25519, new Base64Topping(), Path.of("src/test/resources/crypto"));
    }
}
