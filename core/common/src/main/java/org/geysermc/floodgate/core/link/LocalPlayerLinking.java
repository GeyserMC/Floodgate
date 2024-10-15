/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.link;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.core.database.PendingLinkRepository;
import org.geysermc.floodgate.core.database.PlayerLinkRepository;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;
import org.geysermc.floodgate.core.util.Utils;

@Requires(property = "config.database.enabled", value = "true")
@Requires(property = "config.playerLink.enabled", value = "true")
@Requires(property = "config.playerLink.enableOwnLinking", value = "true")
@Replaces(DisabledPlayerLink.class)
@Named("localLinking")
@Singleton
public class LocalPlayerLinking extends CommonPlayerLink {
    @Inject
    PlayerLinkRepository linkRepository;

    @Inject
    PendingLinkRepository pendingLinkRepository;

    @Override
    public CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId, @NonNull String javaUsername, @NonNull UUID bedrockId) {
        // todo allow it to return self again, probably after the entity rework to interfaces?
        var link = new LinkedPlayer(bedrockId, javaUniqueId, javaUsername);
        return linkRepository.insert(link).thenApply(v -> link);
    }

    @Override
    public CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID uuid) {
        return linkRepository.findByBedrockIdOrJavaUniqueId(uuid, uuid);
    }

    @Override
    public CompletableFuture<Boolean> isLinked(@NonNull UUID uuid) {
        return linkRepository.existsByBedrockIdOrJavaUniqueId(uuid, uuid);
    }

    @Override
    public CompletableFuture<Void> unlink(@NonNull UUID uuid) {
        return linkRepository.deleteByBedrockIdOrJavaUniqueId(uuid, uuid);
    }

    @Override
    public CompletableFuture<Void> createJavaLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code) {
        var linkRequest = new LinkRequest(javaUniqueId, javaUsername, null, bedrockUsername, code);
        return pendingLinkRepository.insert(linkRequest);
    }

    @Override
    public CompletableFuture<Void> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId,
            @NonNull String bedrockUsername,
            @NonNull String javaUsername,
            @NonNull String code) {
        var linkRequest = new LinkRequest(null, javaUsername, bedrockUniqueId, bedrockUsername, code);
        return pendingLinkRepository.insert(linkRequest);
    }

    @Override
    public CompletableFuture<String> createBedrockLinkRequest(
            @NonNull UUID bedrockUniqueId, @NonNull String bedrockUsername) {
        String code = Utils.generateCode(6); // extra long since there is no Java username validation

        var linkRequest = new LinkRequest(null, null, bedrockUniqueId, bedrockUsername, code);
        return pendingLinkRepository.insert(linkRequest).thenApply(v -> code);
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForBedrock(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        return pendingLinkRepository.getAndInvalidateLinkRequestForBedrock(javaUsername, bedrockUsername, code);
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(
            @NonNull String javaUsername, @NonNull String bedrockUsername, @NonNull String code) {
        return pendingLinkRepository.getAndInvalidateLinkRequestForJava(javaUsername, bedrockUsername, code);
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequestForJava(@NonNull String bedrockUsername, @NonNull String code) {
        return pendingLinkRepository.getAndInvalidateLinkRequestForJava(bedrockUsername, code);
    }
}
