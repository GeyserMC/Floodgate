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

package org.geysermc.floodgate.core.http.link;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.UUID;
import org.geysermc.floodgate.core.util.Utils;

@Serdeable
public record LinkedPlayer(
        @JsonbProperty("bedrock_id")
        @Nullable Long xuid,
        @Nullable String gamertag,
        @JsonbProperty("java_id")
        @Nullable UUID uuid,
        @JsonbProperty("java_name")
        @Nullable String username
) {
    public boolean isLinked() {
        // everything will be null when the player is not linked, since we return an empty object.
        // but it's sufficient to check if one of them is null
        return uuid != null;
    }

    public org.geysermc.floodgate.core.database.entity.LinkedPlayer toDatabase() {
        if (!isLinked()) {
            return null;
        }

        return new org.geysermc.floodgate.core.database.entity.LinkedPlayer(Utils.toFloodgateUniqueId(xuid), uuid, username);
    }
}
