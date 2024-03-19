package org.geysermc.floodgate.core.http.downloads;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record LatestBuildResult(int build) {
}
