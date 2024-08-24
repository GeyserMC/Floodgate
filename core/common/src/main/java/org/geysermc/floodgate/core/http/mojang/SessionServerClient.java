package org.geysermc.floodgate.core.http.mojang;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Client("https://sessionserver.mojang.com/session/minecraft")
@Header(name = USER_AGENT, value = "${http.userAgent}")
public interface SessionServerClient {
    @Get("/profile/{uuid}?unsigned=false")
    CompletableFuture<ProfileWithProperties> profileWithProperties(@NotNull UUID uuid);
}
