/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
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
                    "Neither a public not a private key has been provided. Make sure you copied a key.");
        }
        if (keyPair.getPrivate() != null) ensureAlgorithm(algorithmName, keyPair.getPrivate());
        if (keyPair.getPublic() != null) ensureAlgorithm(algorithmName, keyPair.getPublic());
        this.keyPair = keyPair;
    }
}
