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

import java.security.KeyPair;
import java.util.Objects;

public abstract class DataCodecKeyPair extends DataCodec<KeyPair> {
    protected final String algorithmName;
    protected KeyPair keyPair;

    public DataCodecKeyPair(String algorithmName) {
        this.algorithmName = Objects.requireNonNull(algorithmName);
    }

    @Override
    public void init(KeyPair keyPair) {
        if (keyPair.getPrivate() == null && keyPair.getPublic() == null) {
            throw new IllegalArgumentException(
                    "Neither a public not a private key has been provided. Make sure you copied a key."
            );
        }
        if (keyPair.getPrivate() != null)
            ensureAlgorithm(algorithmName, keyPair.getPrivate());
        if (keyPair.getPublic() != null)
            ensureAlgorithm(algorithmName, keyPair.getPublic());
        this.keyPair = keyPair;
    }
}
