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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

final class AesKeyProducerTest {
    @Test
    void produceSingle() {
        var keys = new AesKeyProducer().produce();
        assertEquals(1, keys.size());
        var key = keys.get(0);
        assertInstanceOf(SecretKey.class, key);
        assertEquals("AES", key.getAlgorithm());
    }

    @Test
    void produceUnique() {
        var sampleSize = 5;

        var producer = new AesKeyProducer();
        var distinctKeys =
                Stream.generate(() -> producer.produce().get(0))
                        .limit(sampleSize)
                        .map(key -> Arrays.toString(key.getEncoded()))
                        .distinct();
        assertEquals(sampleSize, distinctKeys.count());
    }
}
