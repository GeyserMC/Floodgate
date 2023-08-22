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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

final class AesDataCodecTest {
    @ParameterizedTest
    @CsvSource({
            "+/XzOuSMr6JAETpBBsgttA==, Hello world!",
            "i0Z5ENhHiwe6Xn3HxJkdfw==, Cya :o"
    })
    void roundtrip(ArgumentsAccessor arguments) throws Exception {
        var messageBuffer = ByteBuffer.wrap(arguments.getString(1).getBytes(StandardCharsets.UTF_8));

        var codec = new AesDataCodec();
        codec.init(new AesKeyCodec().decode(arguments.getString(0).getBytes(StandardCharsets.UTF_8)));

        var encoded = codec.encode(messageBuffer);
        assertEquals(2, encoded.size()); // iv and data
        assertEquals(12, encoded.get(0).remaining()); // iv = 12 bytes
        assertEquals(0, messageBuffer.remaining()); // input should be consumed
        messageBuffer.position(0); // reset input so we can verify it

        var decoded = codec.decode(encoded);
        assertEquals(0, messageBuffer.compareTo(decoded));

        // decode input should be consumed
        assertEquals(0, encoded.get(0).remaining());
        assertEquals(0, encoded.get(1).remaining());
    }

    @ParameterizedTest
    @CsvSource({
            "+/XzOuSMr6JAETpBBsgttA==, Hello world!",
    })
    void encodeIvUniqueness(ArgumentsAccessor arguments) {
        var sampleSize = 5;
        var keyBytes = arguments.getString(0).getBytes(StandardCharsets.UTF_8);
        var messageBytes = arguments.getString(1).getBytes(StandardCharsets.UTF_8);

        var codec = new AesDataCodec();
        codec.init(new AesKeyCodec().decode(keyBytes));

        var generatedIvs = Stream.generate(() -> {
                    try {
                        return codec.encode(ByteBuffer.wrap(messageBytes));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .limit(sampleSize)
                .map(encoded -> StandardCharsets.UTF_8.decode(encoded.get(0)))
                .distinct();
        assertEquals(sampleSize, generatedIvs.count());
    }

    @ParameterizedTest
    @CsvSource({
            "+/XzOuSMr6JAETpBBsgttA==, Hello world!, Dc38Ad2W11gr2hqJ, nu187LoTVbevPEImX+4dPk773/Q+kZoJLh84iw==",
            "+/XzOuSMr6JAETpBBsgttA==, Hello world!, 9VsF6QkrCIQY96Aj, LNJE5xmOQn632x2N/H91MghOtmY1F4bRm98LlQ==",
            "i0Z5ENhHiwe6Xn3HxJkdfw==, Cya :o, CNgdhKbZiA3M1yb/, m81R2ImpuB1bawcjSZgRxa+O5c63vQ==",
            "i0Z5ENhHiwe6Xn3HxJkdfw==, Cya :o, 9jL7BFVzEA/OpKDs, fB7XQb4aPIjp16aZoc+D8hjFxYOZTQ=="
    })
    void decode(ArgumentsAccessor arguments) throws Exception {
        var keyBytes = Base64.getDecoder().decode(arguments.getString(0));
        var messageBytes = arguments.getString(1).getBytes(StandardCharsets.UTF_8);
        var iv = Base64.getDecoder().decode(arguments.getString(2));
        var encryptedData = Base64.getDecoder().decode(arguments.getString(3));

        var codec = new AesDataCodec();
        codec.init(new AesKeyCodec().decode(keyBytes));

        var decrypted = codec.decode(List.of(ByteBuffer.wrap(iv), ByteBuffer.wrap(encryptedData)));
        assertEquals(0, ByteBuffer.wrap(messageBytes).compareTo(decrypted));
    }
}
