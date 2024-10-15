/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.crypto.ed25519;

import org.geysermc.floodgate.core.crypto.DataCodecUsingSignature;

public final class Ed25519DataCodec extends DataCodecUsingSignature {
    public Ed25519DataCodec() {
        super("EdDSA", "Ed25519");
    }
}
