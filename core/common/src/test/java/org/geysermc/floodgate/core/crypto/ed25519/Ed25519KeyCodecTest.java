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

package org.geysermc.floodgate.core.crypto.ed25519;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

final class Ed25519KeyCodecTest {
    private static final Ed25519KeyCodec codec = new Ed25519KeyCodec();

    @ParameterizedTest
    @ValueSource(strings = {
            "MC4CAQAwBQYDK2VwBCIEINFyuLU8O7U/w4nkC5RCzTb2BnlUy8kjo1jwCkluqgYZ",
            "MC4CAQAwBQYDK2VwBCIEII49NjdgnRL/EZsat0qsx1owAkEMj3rtLbNpjd9mbKSf"
    })
    void roundtripPrivate(String key) {
        var encodedKey = key.getBytes(StandardCharsets.UTF_8);
        var decoded = codec.decode(encodedKey, true);
        var encoded = codec.encode(decoded);
        assertArrayEquals(encodedKey, encoded);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MCowBQYDK2VwAyEA3lAOkEnih8ucexsKDtJb+eiwmMZgSLcCWNjSm+RCF9w=",
            "MCowBQYDK2VwAyEAepV2vnvTx68hraRTtF8jKK8POX/60i3jVMHc9BxEVkE="
    })
    void roundtripPublic(String key) {
        var encodedKey = key.getBytes(StandardCharsets.UTF_8);
        var decoded = codec.decode(encodedKey, false);
        var encoded = codec.encode(decoded);
        assertArrayEquals(encodedKey, encoded);
    }

    @ParameterizedTest
    @CsvSource({
            "MC4CAQAwBQYDK2VwBCIEINFyuLU8O7U/w4nkC5RCzTb2BnlUy8kjo1jwCkluqgYZ, MCowBQYDK2VwAyEA3lAOkEnih8ucexsKDtJb+eiwmMZgSLcCWNjSm+RCF9w=",
            "MC4CAQAwBQYDK2VwBCIEII49NjdgnRL/EZsat0qsx1owAkEMj3rtLbNpjd9mbKSf, MCowBQYDK2VwAyEAepV2vnvTx68hraRTtF8jKK8POX/60i3jVMHc9BxEVkE="
    })
    void decodeFromDirectory(ArgumentsAccessor encodedKeyPair, @TempDir Path tempDir) throws IOException {
        var privateKeyFileContent = encodedKeyPair.getString(0).getBytes(StandardCharsets.UTF_8);
        var publicKeyFileContent = encodedKeyPair.getString(1).getBytes(StandardCharsets.UTF_8);

        Files.write(tempDir.resolve("floodgate-private.key"), privateKeyFileContent);
        Files.write(tempDir.resolve("floodgate-public.der"), publicKeyFileContent);

        var decoded = codec.decode(tempDir);
        assertArrayEquals(privateKeyFileContent, codec.encode(decoded.getPrivate()));
        assertArrayEquals(publicKeyFileContent, codec.encode(decoded.getPublic()));
    }

    @ParameterizedTest
    @CsvSource({
            "MC4CAQAwBQYDK2VwBCIEINFyuLU8O7U/w4nkC5RCzTb2BnlUy8kjo1jwCkluqgYZ, MCowBQYDK2VwAyEA3lAOkEnih8ucexsKDtJb+eiwmMZgSLcCWNjSm+RCF9w=",
            "MC4CAQAwBQYDK2VwBCIEII49NjdgnRL/EZsat0qsx1owAkEMj3rtLbNpjd9mbKSf, MCowBQYDK2VwAyEAepV2vnvTx68hraRTtF8jKK8POX/60i3jVMHc9BxEVkE="
    })
    void encodeToDirectory(ArgumentsAccessor encodedKeyPair, @TempDir Path tempDir) throws IOException {
        var privateKeyFileContent = encodedKeyPair.getString(0).getBytes(StandardCharsets.UTF_8);
        var publicKeyFileContent = encodedKeyPair.getString(1).getBytes(StandardCharsets.UTF_8);

        var privateKey = (PrivateKey) codec.decode(privateKeyFileContent, true);
        var publicKey = (PublicKey) codec.decode(publicKeyFileContent, false);

        codec.encode(new KeyPair(publicKey, privateKey), tempDir);

        assertArrayEquals(privateKeyFileContent, Files.readAllBytes(tempDir.resolve("floodgate-private.key")));
        assertArrayEquals(publicKeyFileContent, Files.readAllBytes(tempDir.resolve("floodgate-public.der")));
    }
}
