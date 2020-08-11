package org.geysermc.floodgate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class BungeeFloodgateConfig extends FloodgateConfig {
    @JsonProperty(value = "send-floodgate-data")
    @Getter private boolean sendFloodgateData;

    @JsonProperty(value = "always-force-offline-uuids")
    @Getter private boolean forceOfflineUuids;
}
