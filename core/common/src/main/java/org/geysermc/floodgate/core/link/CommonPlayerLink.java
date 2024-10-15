/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.link;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;

public abstract class CommonPlayerLink {
    @Getter
    private boolean enabled;

    private boolean allowLinking;
    private long verifyLinkTimeout;
    private boolean allowCreateLinkRequest;

    @Inject
    public void commonInit(FloodgateConfig config) {
        FloodgateConfig.PlayerLinkConfig linkConfig = config.playerLink();
        enabled = linkConfig.enabled();
        allowLinking = linkConfig.allowed();
        verifyLinkTimeout = linkConfig.linkCodeTimeout();
        allowCreateLinkRequest = linkConfig.allowCreateLinkRequest();
    }

    public boolean allowCreateLinkRequest() {
        return allowCreateLinkRequest;
    }

    public long verifyLinkTimeout() {
        return verifyLinkTimeout;
    }

    public abstract CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId, @NonNull String javaUsername, @NonNull UUID bedrockId);

    public abstract CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID uuid);

    public abstract CompletableFuture<Boolean> isLinked(@NonNull UUID uuid);

    public abstract CompletableFuture<Void> unlink(@NonNull UUID uuid);

    public abstract CompletableFuture<Void> createJavaLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code);

    public abstract CompletableFuture<Void> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId,
            @NonNull String bedrockUsername,
            @NonNull String javaUsername,
            @NonNull String code);

    public abstract CompletableFuture<String> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId, @NonNull String bedrockUsername);

    public abstract CompletableFuture<LinkRequest> linkRequestForBedrock(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code);

    public abstract CompletableFuture<LinkRequest> linkRequestForJava(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code);

    public abstract CompletableFuture<LinkRequest> linkRequestForJava(
            @NonNull String bedrockUsername, @NonNull String code);

    public PlayerLinkState state() {
        return new PlayerLinkState(enabled && allowLinking);
    }

    public record PlayerLinkState(boolean localLinkingActive, boolean globalLinkingEnabled) {
        public PlayerLinkState(boolean localLinkingActive) {
            this(localLinkingActive, false);
        }
    }
}
