/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.skin.SkinDataImpl;
import org.geysermc.floodgate.util.HttpClient.HttpResponse;

public class MojangUtils {

    private static final String SESSION_SERVER =
            "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private static final Cache<UUID, SkinData> SKIN_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    public static @NonNull SkinData getSkinCached(
            HttpClient httpClient,
            UUID playerId
    ) throws ExecutionException {
        return SKIN_CACHE.get(playerId, () -> getSkin(httpClient, playerId));
    }

    public static @NonNull SkinData getSkin(HttpClient httpClient, UUID playerId) {
        final HttpResponse<JsonObject> httpResponse = httpClient.get(String.format(SESSION_SERVER, playerId.toString()));

        if (httpResponse.getHttpCode() != 200) {
            return SkinDataImpl.DEFAULT_SKIN;
        }

        JsonObject response = httpResponse.getResponse();

        if (response == null) {
            return SkinDataImpl.DEFAULT_SKIN;
        }

        JsonArray properties = response.getAsJsonArray("properties");

        if (properties.size() == 0) {
            return SkinDataImpl.DEFAULT_SKIN;
        }

        for (JsonElement property : properties) {
            if (!property.isJsonObject()) {
                continue;
            }

            JsonObject propertyObject = property.getAsJsonObject();

            if (!propertyObject.has("name")
                    || !propertyObject.has("value")
                    || !propertyObject.has("signature")
                    || !propertyObject.get("name").getAsString().equals("textures")) {
                continue;
            }

            return new SkinDataImpl(
                    propertyObject.get("value").getAsString(),
                    propertyObject.get("signature").getAsString()
            );
        }

        return SkinDataImpl.DEFAULT_SKIN;
    }
}
