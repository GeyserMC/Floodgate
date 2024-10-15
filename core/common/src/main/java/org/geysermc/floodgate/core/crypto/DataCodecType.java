/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
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

    private static final List<DataCodecType<?>> TYPES = new ArrayList<>() {
        {
            add(AES);
            add(ED25519);
        }
    };

    private final String name;
    private final KeyProducer keyProducer;
    private final KeyCodec<K> keyCodec;
    private final Supplier<DataCodec<K>> dataCodec;

    DataCodecType(String name, KeyProducer keyProducer, KeyCodec<K> keyCodec, Supplier<DataCodec<K>> dataCodec) {
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
