package org.geysermc.floodgate.core.http.minecraft;

import io.micronaut.serde.annotation.Serdeable;
import org.checkerframework.checker.nullness.qual.NonNull;

@Serdeable
public record ProfileResult(@NonNull String id, @NonNull String name) {
}
