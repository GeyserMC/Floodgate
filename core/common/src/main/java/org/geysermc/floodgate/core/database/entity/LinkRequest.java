/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.database.entity;

import java.time.Instant;
import java.util.UUID;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Key;

@Entity("LinkRequests")
public record LinkRequest(
        @Key UUID javaUniqueId,
        String javaUsername,
        @Key UUID bedrockUniqueId,
        String bedrockUsername,
        String linkCode,
        long requestTime) {

    public LinkRequest(
            UUID javaUniqueId, String javaUsername, UUID bedrockUniqueId, String bedrockUsername, String linkCode) {
        this(
                javaUniqueId,
                javaUsername,
                bedrockUniqueId,
                bedrockUsername,
                linkCode,
                Instant.now().getEpochSecond());
    }

    public boolean isExpired(long linkTimeout) {
        long timePassed = Instant.now().getEpochSecond() - requestTime;
        return timePassed > linkTimeout;
    }

    public LinkRequest withJava(UUID javaUniqueId, String javaUsername) {
        return new LinkRequest(javaUniqueId, javaUsername, bedrockUniqueId, bedrockUsername, linkCode);
    }

    public LinkRequest withBedrockUniqueId(UUID bedrockUniqueId) {
        return new LinkRequest(javaUniqueId, javaUsername, bedrockUniqueId, bedrockUsername, linkCode);
    }
}
