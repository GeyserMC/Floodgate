/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.crypto;

import java.nio.ByteBuffer;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

public class DataCodecUsingSignature extends DataCodecKeyPair {
    private final String signatureAlgorithm;

    public DataCodecUsingSignature(String algorithmName, String signatureAlgorithm) {
        super(algorithmName);
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
    public List<ByteBuffer> encode(ByteBuffer plainText) throws Exception {
        if (keyPair.getPrivate() == null) {
            throw new IllegalStateException("Cannot sign data with a public key. Did you copy the right key?");
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
            throw new IllegalStateException("Cannot verify data with a private key. Did you copy the right key?");
        }
        ensureSectionCount(2, signatureAlgorithm, dataSections);
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
        } catch (SignatureException ignored) {
        }

        if (!valid) {
            throw new IllegalArgumentException("The given signature is not valid");
        }
        return plainData;
    }
}
