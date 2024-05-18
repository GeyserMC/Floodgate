package org.geysermc.floodgate.core.http.mojang;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@Serdeable
public record ProfileWithProperties(String id, String name, List<ProfileProperty> properties) {
    public @Nullable ProfileProperty texture() {
        for (ProfileProperty property : properties) {
            if (property.name().equals("texture")) {
                return property;
            }
        }
        return null;
    }
}
