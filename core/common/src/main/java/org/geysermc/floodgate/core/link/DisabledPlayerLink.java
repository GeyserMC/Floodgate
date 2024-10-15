/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.link;

import io.micronaut.context.annotation.Secondary;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;

/**
 * Simple class used when PlayerLinking is disabled
 */
@Singleton
@Secondary
final class DisabledPlayerLink extends CommonPlayerLink {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public long verifyLinkTimeout() {
        return -1;
    }

    @Override
    public CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId, @NonNull String javaUsername, @NonNull UUID bedrockId) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID uuid) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<Boolean> isLinked(@NonNull UUID uuid) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> unlink(@NonNull UUID uuid) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> createJavaLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId,
            @NonNull String bedrockUsername,
            @NonNull String javaUsername,
            @NonNull String code) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<String> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId, @NonNull String bedrockUsername) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForBedrock(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(@NonNull String bedrockUsername, @NonNull String code) {
        return failedFuture();
    }

    private <U> CompletableFuture<U> failedFuture() {
        return CompletableFuture.failedFuture(
                new IllegalStateException("Cannot perform this action when PlayerLinking is disabled"));
    }
}
