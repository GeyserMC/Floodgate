/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.link.LinkRequestResult;
import org.geysermc.floodgate.util.HttpUtils;
import org.geysermc.floodgate.util.HttpUtils.HttpResponse;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.Utils;

public class GlobalPlayerLinking extends CommonPlayerLink {
    private static final String GET_BEDROCK_LINK = "https://api.geysermc.org/v1/link/bedrock/";

    @Override
    public void load() {
    }

    @Override
    @NonNull
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(@NonNull UUID bedrockId) {
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
                            Utils.getJavaUuid(data.get("bedrockId").getAsLong()));
                },
                getExecutorService());
    }

    @Override
    @NonNull
    public CompletableFuture<Boolean> isLinkedPlayer(@NonNull UUID bedrockId) {
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

    @Override
    @NonNull
    public CompletableFuture<Void> linkPlayer(
            @NonNull UUID bedrockId,
            @NonNull UUID javaId,
            @NonNull String username) {
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<Void> unlinkPlayer(@NonNull UUID javaId) {
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<?> createLinkRequest(
            @NonNull UUID javaId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername) {
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<LinkRequestResult> verifyLinkRequest(
            @NonNull UUID bedrockId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        return failedFuture();
    }

    private <U> CompletableFuture<U> failedFuture() {
        return Utils.failedFuture(new IllegalStateException(
                "Cannot perform this action when Global Linking is enabled"));
    }
}
