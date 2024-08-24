package org.geysermc.floodgate.core.util;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.core.http.mojang.SessionServerClient;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.skin.SkinDataImpl;

@Singleton
public final class MojangUtils {
    @Inject SessionServerClient sessionClient;
    @Inject FloodgateLogger logger;

    public CompletableFuture<SkinData> skinFor(UUID uuid) {
        return sessionClient.profileWithProperties(uuid)
                .thenApply(skin -> {
                    var texture = skin.texture();
                    if (texture == null) {
                        return SkinDataImpl.DEFAULT_SKIN;
                    }
                    return new SkinDataImpl(texture.value(), texture.signature());
                }).exceptionally(exception -> {
                    logger.debug("Unexpected skin fetch error for " + uuid, exception);
                    return SkinDataImpl.DEFAULT_SKIN;
                });
    }
}
