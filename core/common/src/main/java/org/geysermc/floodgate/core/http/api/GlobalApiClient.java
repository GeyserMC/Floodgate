/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.api;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import org.geysermc.floodgate.core.util.Constants;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface GlobalApiClient {
    /**
     * Checks if it can connect to the Global Api, any other status code than 204 or 404 will throw
     */
    @Get("/healthy")
    void health();
}
