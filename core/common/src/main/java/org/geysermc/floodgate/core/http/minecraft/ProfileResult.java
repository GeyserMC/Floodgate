package org.geysermc.floodgate.core.http.minecraft;

import io.avaje.jsonb.Json;
import org.checkerframework.checker.nullness.qual.NonNull;

@Json
public record ProfileResult(@NonNull String id, @NonNull String name) {
}
