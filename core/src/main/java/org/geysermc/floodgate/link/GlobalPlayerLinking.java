/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import static org.geysermc.floodgate.util.Constants.GET_BEDROCK_LINK;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.link.LinkRequestResult;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.util.HttpClient;
import org.geysermc.floodgate.util.HttpClient.DefaultHttpResponse;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.Utils;

@Getter
public class GlobalPlayerLinking extends CommonPlayerLink {
    @Inject
    private HttpClient httpClient;

    private PlayerLink databaseImpl;

    public void setDatabaseImpl(PlayerLink databaseImpl) {
        if (this.databaseImpl == null) {
            this.databaseImpl = databaseImpl;
        }
    }

    @Override
    public void load() {
        if (databaseImpl != null) {
            databaseImpl.load();
        }
    }

    @Override
    public String getName() {
        if (databaseImpl != null) {
            return databaseImpl.getName();
        }
        // Global Linking is integrated
        return null;
    }

    @Override
    public void stop() {
        super.stop();
        if (databaseImpl != null) {
            databaseImpl.stop();
        }
    }

    @Override
    @NonNull
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(@NonNull UUID bedrockId) {
        if (databaseImpl == null) {
            return getLinkedPlayer0(bedrockId);
        }

        return databaseImpl.getLinkedPlayer(bedrockId).thenComposeAsync(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            return getLinkedPlayer0(bedrockId);
        });
    }

    @NonNull
    private CompletableFuture<LinkedPlayer> getLinkedPlayer0(@NonNull UUID bedrockId) {
        ExecutorService executor = getExecutorService();
        if (executor.isShutdown() || executor.isTerminated()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    DefaultHttpResponse response =
                            httpClient.get(GET_BEDROCK_LINK + bedrockId.getLeastSignificantBits());

                    // either the global api is down or it failed to return link
                    if (!response.isCodeOk()) {
                        if (response.getResponse() != null) {
                            getLogger().error(
                                    "Failed to request link for {}: {}",
                                    bedrockId.getLeastSignificantBits(),
                                    response.getResponse().get("message").getAsString()
                            );
                        }
                        return null;
                    }

                    JsonObject data = response.getResponse();

                    JsonElement javaName = data.get("java_name");
                    // javaName will be null when the player isn't linked
                    if (javaName == null) {
                        return null;
                    }

                    return LinkedPlayer.of(
                            javaName.getAsString(),
                            UUID.fromString(data.get("java_id").getAsString()),
                            Utils.getJavaUuid(data.get("bedrock_id").getAsLong()));
                },
                executor);
    }

    @Override
    @NonNull
    public CompletableFuture<Boolean> isLinkedPlayer(@NonNull UUID bedrockId) {
        if (databaseImpl == null) {
            return isLinkedPlayer0(bedrockId);
        }

        return databaseImpl.isLinkedPlayer(bedrockId).thenComposeAsync(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            return isLinkedPlayer0(bedrockId);
        });
    }

    @NonNull
    private CompletableFuture<Boolean> isLinkedPlayer0(@NonNull UUID bedrockId) {
        ExecutorService executor = getExecutorService();
        if (executor.isShutdown() || executor.isTerminated()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    DefaultHttpResponse response =
                            httpClient.get(GET_BEDROCK_LINK + bedrockId.getLeastSignificantBits());

                    if (!response.isCodeOk()) {
                        getLogger().error(
                                "Failed to request link for {}: {}",
                                bedrockId.getLeastSignificantBits(),
                                response.getResponse().get("message").getAsString());
                        return false;
                    }

                    // no link if data is empty, otherwise the player is linked
                    return response.getResponse().entrySet().size() != 0;
                },
                executor);
    }

    // player linking and unlinking now goes through the global player linking server.
    // so individual servers can't register nor unlink players.

    @Override
    @NonNull
    public CompletableFuture<Void> linkPlayer(
            @NonNull UUID bedrockId,
            @NonNull UUID javaId,
            @NonNull String username) {
        if (databaseImpl != null) {
            return databaseImpl.linkPlayer(bedrockId, javaId, username);
        }
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<Void> unlinkPlayer(@NonNull UUID javaId) {
        if (databaseImpl != null) {
            return databaseImpl.unlinkPlayer(javaId);
        }
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<?> createLinkRequest(
            @NonNull UUID javaId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername) {
        if (databaseImpl != null) {
            return databaseImpl.createLinkRequest(javaId, javaUsername, bedrockUsername);
        }
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<LinkRequestResult> verifyLinkRequest(
            @NonNull UUID bedrockId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        if (databaseImpl != null) {
            return databaseImpl.verifyLinkRequest(bedrockId, javaUsername, bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public boolean isEnabledAndAllowed() {
        return databaseImpl != null && databaseImpl.isEnabledAndAllowed();
    }

    private <U> CompletableFuture<U> failedFuture() {
        return Utils.failedFuture(new IllegalStateException(
                "Cannot perform this action when Global Linking is enabled"));
    }
}
