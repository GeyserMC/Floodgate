package org.geysermc.floodgate.core.http.minecraft;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.util.Constants;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface MinecraftClient {
    @Get("/profile/lookup/name/{name}")
    CompletableFuture<@Nullable ProfileResult> profileByName(@NonNull String name);

    @Get("/profile/lookup/{uuid}")
    CompletableFuture<@Nullable ProfileResult> profileByUniqueId(@NonNull UUID uuid);
}
