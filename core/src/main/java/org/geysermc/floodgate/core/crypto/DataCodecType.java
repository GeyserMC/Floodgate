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

import java.util.function.Supplier;
import org.geysermc.floodgate.core.crypto.aes.AesDataCodec;
import org.geysermc.floodgate.core.crypto.aes.AesKeyCodec;
import org.geysermc.floodgate.core.crypto.aes.AesKeyProducer;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519DataCodec;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519KeyCodec;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519KeyProducer;
import org.geysermc.floodgate.core.crypto.rsa.RsaDataCodec;
import org.geysermc.floodgate.core.crypto.rsa.RsaKeyCodec;
import org.geysermc.floodgate.core.crypto.rsa.RsaKeyProducer;

public enum DataCodecType {
    AES(new AesKeyProducer(), new AesKeyCodec(), AesDataCodec::new, false),
    ED25519(new Ed25519KeyProducer(), new Ed25519KeyCodec(), Ed25519DataCodec::new, true),
    RSA(new RsaKeyProducer(), new RsaKeyCodec(), RsaDataCodec::new, true);

    private final KeyProducer keyProducer;
    private final KeyCodec<?> keyCodec;
    private final Supplier<DataCodec> dataCodec;
    private final boolean asymmetrical;

    private static final DataCodecType[] VALUES = values();

    DataCodecType(
            KeyProducer keyProducer,
            KeyCodec<?> keyCodec,
            Supplier<DataCodec> dataCodec,
            boolean asymmetrical
    ) {
        this.keyProducer = keyProducer;
        this.keyCodec = keyCodec;
        this.dataCodec = dataCodec;
        this.asymmetrical = asymmetrical;
    }

    /**
     * Returns the KeyProducer instance for the given type.
     */
    public KeyProducer keyProducer() {
        return keyProducer;
    }

    /**
     * Returns the KeyCodec instance for the given type.
     */
    public KeyCodec<?> keyCodec() {
        return keyCodec;
    }

    /**
     * Returns a new DataCodec instance for the given type.
     */
    public DataCodec dataCodec() {
        return dataCodec.get();
    }

    /**
     * Returns whether the given type is asymmetrical.
     */
    public boolean asymmetrical() {
        return asymmetrical;
    }

    public static DataCodecType byName(String name) {
        for (DataCodecType value : VALUES) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
