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

@Requires(property = "config.playerLink.enableOwnLinking", value = "true")
@Replaces(DisabledPlayerLink.class)
@Named("localLinking")
@Singleton
public class PlayerLinkHibernate extends CommonPlayerLink {
    @Inject PlayerLinkRepository linkRepository;
    @Inject PendingLinkRepository pendingLinkRepository;

    @Override
    public CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull UUID bedrockId
    ) {
        return linkRepository.save(
                new LinkedPlayer()
                        .javaUniqueId(javaUniqueId)
                        .javaUsername(javaUsername)
                        .bedrockId(bedrockId)
        );
    }

    @Override
    public CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                linkRepository.findByBedrockIdOrJavaUniqueId(uuid, uuid).orElse(null)
        );
    }

    @Override
    public CompletableFuture<Boolean> isLinked(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                linkRepository.existsByBedrockIdOrJavaUniqueId(uuid, uuid)
        );
    }

    @Override
    public CompletableFuture<Void> unlink(@NonNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            linkRepository.deleteByBedrockIdOrJavaUniqueId(uuid, uuid);
            return null;
        });
    }

    @Override
    public CompletableFuture<LinkRequest> createLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code
    ) {
        return pendingLinkRepository.save(
                new LinkRequest()
                        .javaUniqueId(javaUniqueId)
                        .javaUsername(javaUsername)
                        .bedrockUsername(bedrockUsername)
                        .linkCode(code)
        );
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequest(@NonNull String javaUsername) {
        return pendingLinkRepository.findByJavaUsername(javaUsername);
    }

    @Override
    public CompletableFuture<Void> invalidateLinkRequest(@NonNull LinkRequest request) {
        return pendingLinkRepository.delete(request);
    }
}
