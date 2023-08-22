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

package org.geysermc.floodgate.core.crypto.topping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

final class Base64ToppingTest {
    private final Base64Topping topping = new Base64Topping();

    @ParameterizedTest
    @CsvSource({
            "AAAA, QUFBQQ==",
            "achbdbhe, YWNoYmRiaGU=",
            "12asj!+_./, MTJhc2ohK18uLw=="
    })
    void encodeSingleSection(ArgumentsAccessor arguments) {
        var decodedBuffer = ByteBuffer.wrap(arguments.getString(0).getBytes(StandardCharsets.UTF_8));
        var encodedBytes = arguments.getString(1).getBytes(StandardCharsets.UTF_8);

        var encoded = topping.encode(List.of(decodedBuffer));
        assertEquals(0, ByteBuffer.wrap(encodedBytes).compareTo(encoded));
        assertEquals(0, decodedBuffer.remaining());
    }

    @ParameterizedTest
    @CsvSource({
            "AAAA, QUFBQQ==",
            "achbdbhe, YWNoYmRiaGU=",
            "12asj!+_./, MTJhc2ohK18uLw=="
    })
    void decodeSingleSection(ArgumentsAccessor arguments) {
        var decodedBytes = arguments.getString(0).getBytes(StandardCharsets.UTF_8);
        var encodedBytes = arguments.getString(1).getBytes(StandardCharsets.UTF_8);

        var encodedBuffer = ByteBuffer.wrap(encodedBytes);

        var encoded = topping.decode(encodedBuffer);
        assertEquals(0, encodedBuffer.remaining());
        assertEquals(1, encoded.size());
        assertEquals(0, ByteBuffer.wrap(decodedBytes).compareTo(encoded.get(0)));
    }

    @ParameterizedTest
    @CsvSource({
            "QUFBQQ==, AAAA",
            "YWNoYmRiaGU=!Njc2XiomKQ==, achbdbhe, 676^*&)",
            "MTJhc2ohK18uLw==!aGVsbG8hIQ==!d29vbyQl, 12asj!+_./, hello!!, wooo$%"
    })
    void roundtripVarySection(ArgumentsAccessor arguments) {
        var encodedDataBuffer = ByteBuffer.wrap(arguments.getString(0).getBytes(StandardCharsets.UTF_8));

        var sections =
                arguments.toList().stream()
                        .skip(1)
                        .map(e -> (String) e)
                        .map(e -> ByteBuffer.wrap(e.getBytes(StandardCharsets.UTF_8)))
                        .toList();

        var encoded = topping.encode(sections);
        assertEquals(0, encodedDataBuffer.compareTo(encoded));

        sections.forEach(buffer -> {
            assertEquals(0, buffer.remaining());
            buffer.position(0);
        });

        var decoded = topping.decode(encoded);

        assertEquals(0, encoded.remaining());

        assertEquals(sections.size(), decoded.size());
        for (int i = 0; i < sections.size(); i++) {
            assertEquals(0, sections.get(i).compareTo(decoded.get(i)));
        }
    }
}
