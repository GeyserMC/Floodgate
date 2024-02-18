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

package org.geysermc.floodgate.core.link;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;
import org.geysermc.floodgate.core.http.link.GlobalLinkClient;

@Requires(property = "config.playerLink.enabled", value = "true")
@Requires(property = "config.playerLink.enableGlobalLinking", value = "true")
@Primary
@Singleton
@Getter
public class GlobalPlayerLinking extends CommonPlayerLink {
    @Inject GlobalLinkClient linkClient;

    private CommonPlayerLink database;

    @Inject
    void init(@Named("localLinking") BeanProvider<CommonPlayerLink> databaseImpl) {
        this.database = databaseImpl.orElse(null);
    }

    @Override
    @NonNull
    public CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID bedrockId) {
        if (database == null) {
            return getLinkedPlayer0(bedrockId);
        }

        return database.fetchLink(bedrockId).thenCompose(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            return getLinkedPlayer0(bedrockId);
        });
    }

    @NonNull
    private CompletableFuture<LinkedPlayer> getLinkedPlayer0(@NonNull UUID bedrockId) {
        return linkClient.bedrockLink(bedrockId.getLeastSignificantBits())
                .thenApply(org.geysermc.floodgate.core.http.link.LinkedPlayer::toDatabase);
    }

    @Override
    @NonNull
    public CompletableFuture<Boolean> isLinked(@NonNull UUID bedrockId) {
        if (database == null) {
            return isLinkedPlayer0(bedrockId);
        }

        return database.isLinked(bedrockId).thenCompose(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            return isLinkedPlayer0(bedrockId);
        });
    }

    @NonNull
    private CompletableFuture<Boolean> isLinkedPlayer0(@NonNull UUID bedrockId) {
        return getLinkedPlayer0(bedrockId).thenApply(Objects::nonNull);
    }

    // player linking and unlinking now goes through the global player linking server.
    // so individual servers can't register nor unlink players.

    @Override
    @NonNull
    public CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull UUID bedrockId
    ) {
        if (database != null) {
            return database.addLink(javaUniqueId, javaUsername, bedrockId);
        }
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<Void> unlink(@NonNull UUID javaUniqueId) {
        if (database != null) {
            return database.unlink(javaUniqueId);
        }
        return failedFuture();
    }

    @Override
    @NonNull
    public CompletableFuture<LinkRequest> createLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code
    ) {
        if (database != null) {
            return database.createLinkRequest(javaUniqueId, javaUsername, bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequest(@NonNull String javaUsername) {
        if (database != null) {
            return database.linkRequest(javaUsername);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> invalidateLinkRequest(@NonNull LinkRequest request) {
        if (database != null) {
            return database.invalidateLinkRequest(request);
        }
        return failedFuture();
    }

    @Override
    public PlayerLinkState state() {
        return new PlayerLinkState(database != null && database.state().localLinkingActive(), true);
    }

    private <U> CompletableFuture<U> failedFuture() {
        return CompletableFuture.failedFuture(new IllegalStateException(
                "Cannot perform this action when Global Linking is enabled"
        ));
    }
}
