package org.geysermc.floodgate.core.http;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.http.minecraft.MinecraftClient;
import org.geysermc.floodgate.core.http.minecraft.ProfileResult;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.core.util.Utils;

@Singleton
public final class ProfileFetcher {
    @Inject MinecraftClient minecraftClient;
    @Inject XboxClient xboxClient;

    public CompletableFuture<@Nullable ProfileAudience> fetchUniqueIdFor(String username) {
        return minecraftClient.profileByName(username).thenApply(this::convert);
    }

    public CompletableFuture<@Nullable ProfileAudience> fetchUsernameFor(UUID uniqueId) {
        return minecraftClient.profileByUniqueId(uniqueId).thenApply(this::convert);
    }

    public CompletableFuture<@Nullable ProfileAudience> fetchXuidFor(String gamertag) {
        return xboxClient.xuidByGamertag(gamertag).thenApply(result -> {
            var xuid = result.xuid();
            if (xuid == null) {
                return null;
            }
            return new ProfileAudience(Utils.toFloodgateUniqueId(xuid), gamertag);
        });
    }

    public CompletableFuture<@Nullable ProfileAudience> fetchGamertagFor(long xuid) {
        return xboxClient.gamertagByXuid(xuid).thenApply(result -> {
            var gamertag = result.gamertag();
            if (gamertag == null) {
                return null;
            }
            return new ProfileAudience(Utils.toFloodgateUniqueId(xuid), gamertag);
        });
    }

    public CompletableFuture<@Nullable ProfileAudience> fetchGamertagFor(UUID xuid) {
        return fetchGamertagFor(xuid.getLeastSignificantBits());
    }

    private @Nullable ProfileAudience convert(@Nullable ProfileResult result) {
        if (result == null) {
            return null;
        }
        return new ProfileAudience(Utils.fromShortUniqueId(result.id()), result.name());
    }
}
