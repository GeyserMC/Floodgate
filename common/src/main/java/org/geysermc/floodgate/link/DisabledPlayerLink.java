/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.LinkRequestResult;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.util.LinkedPlayer;

/**
 * Simple class used when PlayerLinking is disabled. This class has been made because Floodgate
 * doesn't have a default PlayerLink implementation anymore and {@link FloodgateApi#getPlayerLink()}
 * returning null} is also not an option.
 */
final class DisabledPlayerLink implements PlayerLink {
    @Override
    public void load() {
    }

    @Override
    public CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> isLinkedPlayer(UUID bedrockId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> linkPlayer(UUID bedrockId, UUID javaId, String username) {
        return null;
    }

    @Override
    public CompletableFuture<Void> unlinkPlayer(UUID javaId) {
        return null;
    }

    @Override
    public CompletableFuture<?> createLinkRequest(
            UUID javaId,
            String javaUsername,
            String bedrockUsername
    ) {
        return null;
    }

    @Override
    public CompletableFuture<LinkRequestResult> verifyLinkRequest(
            UUID bedrockId,
            String javaUsername,
            String bedrockUsername,
            String code
    ) {
        return null;
    }

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
    public void stop() {
    }
}
