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

import java.nio.ByteBuffer;
import java.security.Key;
import java.util.List;
import org.geysermc.floodgate.core.crypto.topping.Topping;

/**
 * Responsible for data integrity by either signing or encrypting the data.
 * <p>
 * Unlike {@link KeyProducer}, {@link KeyCodec} and {@link Topping} this class does have state.
 * For every key a new instance should be made, or use {@link #init(Object)} to reset that state.
 * </p>
 */
public abstract class DataCodec<K> {
    /**
     * Initializes the instance by giving it the key it needs to sign/verify or encrypt/decrypt data
     *
     * @param key the key used to sign/verify or encrypt/decrypt data
     */
    public abstract void init(K key);

    /**
     * Encodes the given data using the Key provided in {@link #init(Object)}
     *
     * @param plainText the data to encode
     * @return the encoded data
     * @throws Exception when the encoding failed
     */
    public abstract List<ByteBuffer> encode(ByteBuffer plainText) throws Exception;

    /**
     * Decodes the given data using the Key provided in {@link #init(Object)}
     *
     * @param dataSections the data sections to decode
     * @return the decoded data
     * @throws Exception when the decoding failed
     */
    public abstract ByteBuffer decode(List<ByteBuffer> dataSections) throws Exception;

    protected void ensureAlgorithm(String algorithm, Key key) {
        if (!algorithm.equals(key.getAlgorithm())) {
            throw new RuntimeException(String.format(
                    "Algorithm was expected to be %s, but got %s",
                    algorithm, key.getAlgorithm()
            ));
        }
    }

    protected void ensureSectionCount(int expectedSectionCount, String name, List<ByteBuffer> sections) {
        if (sections.size() != expectedSectionCount) {
            throw new IllegalArgumentException(
                    "The " + name + " data codec expects two sections. Is the correct data codec chosen?"
            );
        }
    }
}
