/*
 * Copyright (c) 2019-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.database.entity;

import java.util.UUID;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Key;
import org.geysermc.databaseutils.meta.Length;

@Entity("LinkedPlayers")
public record LinkedPlayer(@Key UUID bedrockId, UUID javaUniqueId, @Length(max = 16) String javaUsername) {}
