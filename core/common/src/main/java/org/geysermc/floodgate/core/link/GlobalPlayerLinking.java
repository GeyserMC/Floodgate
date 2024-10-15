/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
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
    @Inject
    GlobalLinkClient linkClient;

    private CommonPlayerLink database;

    @Inject
    void init(@Named("localLinking") BeanProvider<CommonPlayerLink> databaseImpl) {
        this.database = databaseImpl.orElse(null);
    }

    @Override
    public @NonNull CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID bedrockId) {
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

    private @NonNull CompletableFuture<LinkedPlayer> getLinkedPlayer0(@NonNull UUID bedrockId) {
        return linkClient
                .bedrockLink(bedrockId.getLeastSignificantBits())
                .thenApply(org.geysermc.floodgate.core.http.link.LinkedPlayer::toDatabase);
    }

    @Override
    public @NonNull CompletableFuture<Boolean> isLinked(@NonNull UUID bedrockId) {
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

    private @NonNull CompletableFuture<Boolean> isLinkedPlayer0(@NonNull UUID bedrockId) {
        return getLinkedPlayer0(bedrockId).thenApply(Objects::nonNull);
    }

    // player linking and unlinking now goes through the global player linking server.
    // so individual servers can't register nor unlink players.

    @Override
    public @NonNull CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId, @NonNull String javaUsername, @NonNull UUID bedrockId) {
        if (database != null) {
            return database.addLink(javaUniqueId, javaUsername, bedrockId);
        }
        return failedFuture();
    }

    @Override
    public @NonNull CompletableFuture<Void> unlink(@NonNull UUID javaUniqueId) {
        if (database != null) {
            return database.unlink(javaUniqueId);
        }
        return failedFuture();
    }

    @Override
    public @NonNull CompletableFuture<Void> createJavaLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        if (database != null) {
            return database.createJavaLinkRequest(javaUniqueId, javaUsername, bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId,
            @NonNull String bedrockUsername,
            @NonNull String javaUsername,
            @NonNull String code) {
        if (database != null) {
            return database.createBedrockLinkRequest(bedrockUniqueId, bedrockUsername, javaUsername, code);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<String> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId, @NonNull String bedrockUsername) {
        if (database != null) {
            return database.createBedrockLinkRequest(bedrockUniqueId, bedrockUsername);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForBedrock(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        if (database != null) {
            return database.linkRequestForBedrock(javaUsername, bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        if (database != null) {
            return database.linkRequestForJava(javaUsername, bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(@NonNull String bedrockUsername, @NonNull String code) {
        if (database != null) {
            return database.linkRequestForJava(bedrockUsername, code);
        }
        return failedFuture();
    }

    @Override
    public PlayerLinkState state() {
        return new PlayerLinkState(database != null && database.state().localLinkingActive(), true);
    }

    private <U> CompletableFuture<U> failedFuture() {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Cannot perform this action when Global Linking is enabled"));
    }
}
