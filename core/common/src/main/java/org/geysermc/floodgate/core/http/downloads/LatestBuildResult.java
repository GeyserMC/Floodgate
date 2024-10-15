/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.http.downloads;

import io.avaje.jsonb.Json;

@Json
public record LatestBuildResult(int build) {}
