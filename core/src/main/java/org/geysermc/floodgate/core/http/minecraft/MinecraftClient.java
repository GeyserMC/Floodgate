package org.geysermc.floodgate.core.http.minecraft;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Client("https://api.minecraftservices.com/minecraft")
@Header(name = USER_AGENT, value = "${http.userAgent}")
public interface MinecraftClient {
    @Get("/profile/lookup/name/{name}")
    CompletableFuture<@Nullable ProfileResult> profileByName(@NonNull String name);

    @Get("/profile/lookup/{uuid}")
    CompletableFuture<@Nullable ProfileResult> profileByUniqueId(@NonNull UUID uuid);
}
