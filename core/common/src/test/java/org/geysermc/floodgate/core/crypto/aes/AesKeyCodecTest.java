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

package org.geysermc.floodgate.core.crypto.aes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class AesKeyCodecTest {
    private static final AesKeyCodec codec = new AesKeyCodec();

    @ParameterizedTest
    @ValueSource(strings = {"+/XzOuSMr6JAETpBBsgttA==", "i0Z5ENhHiwe6Xn3HxJkdfw=="})
    void roundtrip(String key) {
        var encodedKey = key.getBytes(StandardCharsets.UTF_8);
        var decoded = codec.decode(encodedKey);
        var encoded = codec.encode(decoded);
        assertArrayEquals(encodedKey, encoded);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+/XzOuSMr6JAETpBBsgttA==", "i0Z5ENhHiwe6Xn3HxJkdfw=="})
    void preFloodgate30KeyRoundtrip(String key) {
        var encodedKey = key.getBytes(StandardCharsets.UTF_8);
        // pre-3.0 Floodgate keys were not base64 encoded
        var oldEncodedKey = Base64.getDecoder().decode(encodedKey);
        var decoded = codec.decode(oldEncodedKey);
        var encoded = codec.encode(decoded);
        assertArrayEquals(encodedKey, encoded);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+/XzOuSMr6JAETpBBsgttA==", "i0Z5ENhHiwe6Xn3HxJkdfw=="})
    void decodeFromDirectory(String keyFileContent, @TempDir Path tempDir) throws IOException {
        var keyFileContentBytes = keyFileContent.getBytes(StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("floodgate.key"), keyFileContentBytes);
        var decoded = codec.decode(tempDir);
        assertArrayEquals(keyFileContentBytes, codec.encode(decoded));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+/XzOuSMr6JAETpBBsgttA==", "i0Z5ENhHiwe6Xn3HxJkdfw=="})
    void encodeToDirectory(String keyFileContent, @TempDir Path tempDir) throws IOException {
        var keyFilePath = tempDir.resolve("floodgate.key");
        assertTrue(Files.notExists(keyFilePath));

        var decoded = codec.decode(keyFileContent.getBytes(StandardCharsets.UTF_8));
        codec.encode(decoded, tempDir);
        assertTrue(Files.exists(keyFilePath));
        assertEquals(keyFileContent, Files.readString(keyFilePath, StandardCharsets.UTF_8));
    }
}
