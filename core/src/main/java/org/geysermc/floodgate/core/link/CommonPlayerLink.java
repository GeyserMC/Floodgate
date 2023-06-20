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

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
import org.geysermc.floodgate.core.database.entity.LinkedPlayer;

public abstract class CommonPlayerLink {
    @Getter private boolean enabled;
    @Getter private boolean allowLinking;
    @Getter private long verifyLinkTimeout;

    @Inject
    @Getter(AccessLevel.PROTECTED)
    FloodgateLogger logger;

    @Inject
    @Getter(AccessLevel.PROTECTED)
    FloodgateApi api;

    @Inject
    public void commonInit(FloodgateConfig config) {
        FloodgateConfig.PlayerLinkConfig linkConfig = config.playerLink();
        enabled = linkConfig.enabled();
        allowLinking = linkConfig.allowed();
        verifyLinkTimeout = linkConfig.linkCodeTimeout();
    }

    /**
     * Checks if the given FloodgatePlayer is the player requested in this LinkRequest. This method
     * will check both the real bedrock username {@link FloodgatePlayer#getUsername()} and the
     * edited username {@link FloodgatePlayer#getJavaUsername()} and returns true if one of the two
     * matches.
     *
     * @return true if the given player is the player requested
     */
    public boolean isRequestedPlayer(LinkRequest request, UUID bedrockId) {
        // Java starts the process, Bedrock finishes it. So player can't be null
        var player = api.getPlayer(bedrockId);
        return request.bedrockUsername().equals(player.getUsername()) ||
                request.bedrockUsername().equals(player.getJavaUsername());
    }

    public abstract CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull UUID bedrockId
    );

    public abstract CompletableFuture<LinkedPlayer> fetchLink(@NonNull UUID uuid);

    public abstract CompletableFuture<Boolean> isLinked(@NonNull UUID uuid);

    public abstract CompletableFuture<Void> unlink(@NonNull UUID uuid);

    public abstract CompletableFuture<LinkRequest> createLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code
    );

    public abstract CompletableFuture<LinkRequest> linkRequest(@NonNull String javaUsername);

    public abstract CompletableFuture<Void> invalidateLinkRequest(@NonNull LinkRequest request);

    public boolean isActive() {
        return enabled && allowLinking;
    }
}
