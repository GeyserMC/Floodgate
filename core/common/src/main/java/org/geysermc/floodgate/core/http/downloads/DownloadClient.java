package org.geysermc.floodgate.core.http.downloads;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import jakarta.validation.constraints.NotBlank;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.util.Constants;

@Client("${http.baseUrl.download}")
@Header(name = HttpHeaders.USER_AGENT, value = Constants.USER_AGENT)
public interface DownloadClient {
    @Get("/v2/projects/{project}/versions/latest/builds/latest")
    CompletableFuture<LatestBuildResult> latestBuildFor(@NotBlank String project);
}
