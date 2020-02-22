package org.geysermc.floodgate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class VelocityFloodgateConfig extends FloodgateConfig {
    @JsonProperty(value = "send-floodgate-data")
    @Getter
    private boolean sendFloodgateData;
}
