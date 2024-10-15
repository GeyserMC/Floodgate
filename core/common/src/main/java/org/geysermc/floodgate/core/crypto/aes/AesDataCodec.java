/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.crypto.aes;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.geysermc.floodgate.core.crypto.DataCodec;
import org.geysermc.floodgate.core.crypto.RandomUtils;

public final class AesDataCodec extends DataCodec<SecretKey> {
    private static final String CIPHER_NAME = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BIT_LENGTH = 128;

    private SecureRandom secureRandom;
    private SecretKey key;

    @Override
    public void init(SecretKey key) {
        ensureAlgorithm("AES", key);
        this.key = key;
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
