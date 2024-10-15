/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.mojang;

import io.avaje.jsonb.Json;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Json
public record ProfileProperty(@NonNull String name, @NonNull String value, @Nullable String signature) {}
