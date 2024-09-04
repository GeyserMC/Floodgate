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
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.crypto.aes.AesDataCodec;
import org.geysermc.floodgate.core.crypto.aes.AesKeyCodec;
import org.geysermc.floodgate.core.crypto.aes.AesKeyProducer;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519DataCodec;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519KeyCodec;
import org.geysermc.floodgate.core.crypto.ed25519.Ed25519KeyProducer;

public final class DataCodecType<K> {
    public static final DataCodecType<SecretKey> AES =
            new DataCodecType<>("AES", new AesKeyProducer(), new AesKeyCodec(), AesDataCodec::new);
    public static final DataCodecType<KeyPair> ED25519 =
            new DataCodecType<>("Ed25519", new Ed25519KeyProducer(), new Ed25519KeyCodec(), Ed25519DataCodec::new);

    private static final List<DataCodecType<?>> TYPES = new ArrayList<>() {{
        add(AES);
        add(ED25519);
    }};

    private final String name;
    private final KeyProducer keyProducer;
    private final KeyCodec<K> keyCodec;
    private final Supplier<DataCodec<K>> dataCodec;

    DataCodecType(
            String name,
            KeyProducer keyProducer,
            KeyCodec<K> keyCodec,
            Supplier<DataCodec<K>> dataCodec
    ) {
        this.name = name;
        this.keyProducer = keyProducer;
        this.keyCodec = keyCodec;
        this.dataCodec = dataCodec;
    }

    /**
     * Returns the name of the data codec type
     */
    public String name() {
        return name;
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
    public KeyCodec<K> keyCodec() {
        return keyCodec;
    }

    /**
     * Creates (and initializes) a data codec, for a given directory.
     * @see DataCodec#init(Object)
     * @see KeyCodec#decode(Path)
     * @throws IOException as described in {@link KeyCodec#decode(Path)}
     */
    public DataCodec<K> createDataCodec(Path keyDirectory) throws IOException {
        var codec = dataCodec.get();
        codec.init(keyCodec.decode(keyDirectory));
        return codec;
    }

    /**
     * Gets a data codec type by its name
     *
     * @param name the name (case-insensitive)
     * @return the type, otherwise null
     */
    public static @Nullable DataCodecType<?> byName(String name) {
        for (DataCodecType<?> value : TYPES) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
