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
    public long getVerifyLinkTimeout() {
        return -1;
    }

    @Override
    public boolean isAllowLinking() {
        return false;
    }

    @Override
    public CompletableFuture<LinkedPlayer> addLink(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull UUID bedrockId
    ) {
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
    public CompletableFuture<LinkRequest> createLinkRequest(
            @NonNull UUID javaUniqueId,
            @NonNull String javaUsername,
            @NonNull String bedrockUsername,
            @NonNull String code
    ) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<LinkRequest> linkRequest(@NonNull String javaUsername) {
        return failedFuture();
    }

    @Override
    public CompletableFuture<Void> invalidateLinkRequest(@NonNull LinkRequest request) {
        return failedFuture();
    }

    private <U> CompletableFuture<U> failedFuture() {
        return CompletableFuture.failedFuture(new IllegalStateException(
                "Cannot perform this action when PlayerLinking is disabled"
        ));
    }
}
