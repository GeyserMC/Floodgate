/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.xbox;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.core.util.Constants;
import org.jetbrains.annotations.Range;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface XboxClient {
    @Get("/xuid/{gamertag}")
    CompletableFuture<GetXuidResult> xuidByGamertag(@Range(from = 1, to = 16) @NonNull String gamertag);

    @Get("/gamertag/{xuid}")
    CompletableFuture<GetGamertagResult> gamertagByXuid(long xuid);
}
