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

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.geysermc.floodgate.core.crypto.DataCodec;
import org.geysermc.floodgate.core.crypto.RandomUtils;

public final class AesDataCodec extends DataCodec {
    private static final String CIPHER_NAME = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BIT_LENGTH = 128;

    private SecureRandom secureRandom;
    private SecretKey key;

    @Override
    public void init(Key key) {
        ensureAlgorithm("AES", key);
        this.key = (SecretKey) key;
        this.secureRandom = RandomUtils.secureRandom();
    }

    @Override
    public List<ByteBuffer> encode(ByteBuffer plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_NAME);

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        ByteBuffer output = ByteBuffer.allocate(cipher.getOutputSize(plainText.remaining()));
        cipher.doFinal(plainText, output);
        output.position(0);

        return List.of(ByteBuffer.wrap(iv), output);
    }

    @Override
    public ByteBuffer decode(List<ByteBuffer> ivAndCipherText) throws Exception {
        ensureSectionCount(2, "AES", ivAndCipherText);

        var ivSection = ivAndCipherText.get(0);
        byte[] iv = new byte[ivSection.remaining()];
        ivSection.get(iv);

        var cipherText = ivAndCipherText.get(1);

        var cipher = Cipher.getInstance(CIPHER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BIT_LENGTH, iv));

        var outputBuffer = ByteBuffer.allocate(cipher.getOutputSize(cipherText.remaining()));
        cipher.doFinal(cipherText, outputBuffer);
        outputBuffer.position(0);
        return outputBuffer;
    }
}
