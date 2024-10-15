/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.link;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.util.Constants;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface GlobalLinkClient {
    @Get("/bedrock/{xuid}")
    CompletableFuture<LinkedPlayer> bedrockLink(long xuid);
}
