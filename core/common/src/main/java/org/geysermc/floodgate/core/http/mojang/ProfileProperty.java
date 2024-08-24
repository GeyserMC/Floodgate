package org.geysermc.floodgate.core.http.mojang;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Serdeable
public record ProfileProperty(@NotNull String name, @NotNull String value, @Nullable String signature) {
}
