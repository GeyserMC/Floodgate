package org.geysermc.floodgate.core.http.mojang;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.util.Constants;
import org.jetbrains.annotations.NotNull;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface SessionServerClient {
    @Get("/profile/{uuid}/?unsigned=false")
    CompletableFuture<ProfileWithProperties> profileWithProperties(@NotNull UUID uuid);
}
