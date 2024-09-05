package org.geysermc.floodgate.core.http.downloads;

import io.avaje.jsonb.Json;

@Json
public record LatestBuildResult(int build) {
}
