package org.geysermc.floodgate.core.http.api;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import org.geysermc.floodgate.core.util.Constants;

@Client("${http.baseUrl.api}")
@Header(name = HttpHeaders.USER_AGENT, value = Constants.USER_AGENT)
public interface GlobalApiClient {
    /**
     * Checks if it can connect to the Global Api, any other status code than 204 or 404 will throw
     */
    @Get("/healthy")
    void health();
}
