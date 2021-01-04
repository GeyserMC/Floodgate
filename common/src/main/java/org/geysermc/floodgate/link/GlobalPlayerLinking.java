/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.link;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.LinkRequestResult;
import org.geysermc.floodgate.util.HttpUtils;
import org.geysermc.floodgate.util.HttpUtils.HttpResponse;
import org.geysermc.floodgate.util.LinkedPlayer;

public class GlobalPlayerLinking extends CommonPlayerLink {
    private static final String GET_BEDROCK_LINK = "http://localhost:4000/api/link/bedrock?xuid=";

    @Inject private FloodgateApi api;

    @Override
    public void load() {}

    @Override
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId) {
        return CompletableFuture.supplyAsync(
            () -> {
                HttpResponse response =
                    HttpUtils.get(GET_BEDROCK_LINK + bedrockId.getLeastSignificantBits());

                // both on code != 200 and fails with 200 'success' will be false
                if (!response.getResponse().get("success").getAsBoolean()) {
                    getLogger().error(
                        "Failed to request link for {}: {}",
                        bedrockId.getLeastSignificantBits(),
                        response.getResponse().get("message").getAsString());
                    return null;
                }

                JsonObject data = response.getResponse().getAsJsonObject("data");

                // no link if data is empty
                if (data.size() == 0) {
                    return null;
                }

                return LinkedPlayer.of(
                    data.get("javaName").getAsString(),
                    UUID.fromString(data.get("javaId").getAsString()),
                    api.createJavaPlayerId(data.get("bedrockId").getAsLong()));
            },
            getExecutorService());
    }

    @Override
    public CompletableFuture<Boolean> isLinkedPlayer(UUID bedrockId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    HttpResponse response =
                            HttpUtils.get(GET_BEDROCK_LINK + bedrockId.getLeastSignificantBits());

                    // both on http != 200 and fails with 200 success will be false
                    if (!response.getResponse().get("success").getAsBoolean()) {
                        getLogger().error(
                                "Failed to request link for {}: {}",
                                bedrockId.getLeastSignificantBits(),
                                response.getResponse().get("message").getAsString());
                        return false;
                    }

                    JsonObject data = response.getResponse().getAsJsonObject("data");

                    // no link if data is empty, otherwise the player is linked
                    return data.size() != 0;
                },
                getExecutorService());
    }

    // player linking and unlinking now goes through the global player linking server.
    // so individual servers can't register nor unlink players.

    //todo probably return a failed future instead of returning null?

    @Override
    public CompletableFuture<Void> linkPlayer(UUID bedrockId, UUID javaId, String username) {
        return null;
    }

    @Override
    public CompletableFuture<Void> unlinkPlayer(UUID javaId) {
        return null;
    }

    @Override
    public CompletableFuture<?> createLinkRequest(
            UUID javaId,
            String javaUsername,
            String bedrockUsername
    ) {
        return null;
    }

    @Override
    public CompletableFuture<LinkRequestResult> verifyLinkRequest(
            UUID bedrockId,
            String javaUsername,
            String bedrockUsername,
            String code
    ) {
        return null;
    }
}
