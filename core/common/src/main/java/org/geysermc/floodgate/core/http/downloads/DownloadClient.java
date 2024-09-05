package org.geysermc.floodgate.core.http.downloads;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Headers;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.util.Constants;

@Client
@Headers({"User-Agent: " + Constants.USER_AGENT})
public interface DownloadClient {
    @Get("/v2/projects/{project}/versions/latest/builds/latest")
    CompletableFuture<LatestBuildResult> latestBuildFor(String project);
}
