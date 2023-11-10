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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public abstract class KeyCodecPair implements KeyCodec<KeyPair> {
    protected final String PRIVATE_KEY_NAME = "floodgate-private.key";
    protected final String PUBLIC_KEY_NAME = "floodgate-public.der";

    protected final KeyFactory factory;

    protected KeyCodecPair(String algorithm) {
        try {
            this.factory = KeyFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }
    }

    public KeyPair decode(Path keyDirectory) throws IOException {
        Path privateKeyPath = keyDirectory.resolve(PRIVATE_KEY_NAME);
        PrivateKey privateKey = null;
        if (Files.exists(privateKeyPath)) {
            privateKey = (PrivateKey) decode(Files.readAllBytes(privateKeyPath), true);
        }

        Path publicKeyPath = keyDirectory.resolve(PUBLIC_KEY_NAME);
        PublicKey publicKey = null;
        if (Files.exists(publicKeyPath)) {
            publicKey = (PublicKey) decode(Files.readAllBytes(publicKeyPath), false);
        }

        return new KeyPair(publicKey, privateKey);
    }

    public Key decode(byte[] keyBytes, boolean privateKey) {
        keyBytes = Base64.getDecoder().decode(keyBytes);

        try {
            if (privateKey) {
                return factory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes, algorithm()));
            }
            return factory.generatePublic(new X509EncodedKeySpec(keyBytes, algorithm()));
        } catch (InvalidKeySpecException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void encode(KeyPair keyPair, Path keyDirectory) throws IOException {
        if (keyPair.getPrivate() != null) {
            Files.write(keyDirectory.resolve(PRIVATE_KEY_NAME), encode(keyPair.getPrivate()));
        }
        if (keyPair.getPublic() != null) {
            Files.write(keyDirectory.resolve(PUBLIC_KEY_NAME), encode(keyPair.getPublic()));
        }
    }

    public byte[] encode(Key keyData) {
        return Base64.getEncoder().encode(keyData.getEncoded());
    }

    protected String algorithm() {
        return factory.getAlgorithm();
    }
}
