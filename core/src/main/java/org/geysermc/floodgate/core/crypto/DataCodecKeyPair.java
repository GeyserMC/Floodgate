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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;
import java.util.Objects;

public abstract class DataCodecKeyPair extends DataCodec {
    private final String algorithmName;
    private final String signatureAlgorithm;

    private KeyPair keyPair;

    public DataCodecKeyPair(String algorithmName, String signatureAlgorithm) {
        this.algorithmName = Objects.requireNonNull(algorithmName);
        this.signatureAlgorithm = Objects.requireNonNull(signatureAlgorithm);
    }

    @Override
    public void init(Key key) {
        ensureAlgorithm(algorithmName, key);
        if (key instanceof PrivateKey privateKey) {
            this.keyPair = new KeyPair(null, privateKey);
        } else {
            this.keyPair = new KeyPair((PublicKey) key, null);
        }
    }

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

    @Override
    public List<ByteBuffer> encode(ByteBuffer plainText) throws Exception {
        if (keyPair.getPrivate() == null) {
            throw new IllegalStateException(
                    "Cannot sign data with a public key. Did you copy the right key?"
            );
        }

        var plainData = plainText.duplicate();

        var signature = Signature.getInstance(signatureAlgorithm);
        signature.initSign(keyPair.getPrivate());
        signature.update(plainText);
        var signatureBuffer = ByteBuffer.wrap(signature.sign());

        return List.of(plainData, signatureBuffer);
    }

    @Override
    public ByteBuffer decode(List<ByteBuffer> dataSections) throws Exception {
        if (keyPair.getPublic() == null) {
            throw new IllegalStateException(
                    "Cannot verify data with a private key. Did you copy the right key?"
            );
        }
        ensureSectionCount(2, algorithmName, dataSections);
        var data = dataSections.get(0);
        var signatureBuffer = dataSections.get(1);

        var plainData = data.duplicate();

        var signature = Signature.getInstance(signatureAlgorithm);
        signature.initVerify(keyPair.getPublic());
        signature.update(data);

        var signatureBytes = new byte[signatureBuffer.remaining()];
        signatureBuffer.get(signatureBytes);

        var valid = false;
        try {
            valid = signature.verify(signatureBytes);
        } catch (SignatureException ignored) {}

        if (!valid) {
            throw new IllegalArgumentException("The given signature is not valid");
        }
        return plainData;
    }
}
