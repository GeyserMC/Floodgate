/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.link;

import io.avaje.jsonb.Json;
import io.avaje.jsonb.Json.Property;
import jakarta.annotation.Nullable;
import java.util.UUID;
import org.geysermc.floodgate.core.util.Utils;

@Json
public record LinkedPlayer(
        @Property("bedrock_id") @Nullable Long xuid,
        @Nullable String gamertag,
        @Property("java_id") @Nullable UUID uuid,
        @Property("java_name") @Nullable String username) {
    public boolean isLinked() {
        // everything will be null when the player is not linked, since we return an empty object.
        // but it's sufficient to check if one of them is null
        return uuid != null;
    }

    public org.geysermc.floodgate.core.database.entity.LinkedPlayer toDatabase() {
        if (!isLinked()) {
            return null;
        }

        return new org.geysermc.floodgate.core.database.entity.LinkedPlayer(
                Utils.toFloodgateUniqueId(xuid), uuid, username);
    }
}
