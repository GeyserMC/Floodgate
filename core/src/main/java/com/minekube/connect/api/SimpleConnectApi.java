/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package com.minekube.connect.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.player.ConnectPlayer;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleConnectApi implements ConnectApi {
    private final Map<UUID, ConnectPlayer> players = Maps.newConcurrentMap();
    private final Cache<UUID, ConnectPlayer> pendingRemove =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .build();

    private final ConnectLogger logger;

    @Override
    public Collection<ConnectPlayer> getPlayers() {
        return ImmutableSet.copyOf(players.values());
    }

    @Override
    public int getPlayerCount() {
        return players.size();
    }

    @Override
    public boolean isConnectPlayer(UUID uuid) {
        return getPlayer(uuid) != null;
    }

    @Override
    public ConnectPlayer getPlayer(UUID uuid) {
        ConnectPlayer player = players.get(uuid);
        if (player != null) {
            return player;
        }

        // and don't forget the pending remove players
        return getPendingRemovePlayer(uuid);
    }

    public ConnectPlayer addPlayer(ConnectPlayer player) {
        return players.put(player.getUniqueId(), player);
    }

    /**
     * This method is invoked when the player is no longer on the server, but the related platform-
     * dependant event hasn't fired yet
     */
    public boolean setPendingRemove(ConnectPlayer player) {
        pendingRemove.put(player.getUniqueId(), player);
        return players.remove(player.getUniqueId(), player);
    }

    public void playerRemoved(UUID uuid) {
        pendingRemove.invalidate(uuid);
        players.remove(uuid);
    }

    private ConnectPlayer getPendingRemovePlayer(UUID uuid) {
        return pendingRemove.getIfPresent(uuid);
    }
}
