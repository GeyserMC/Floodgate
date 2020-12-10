/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.FloodgatePlayerImpl;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;

@RequiredArgsConstructor
public class SimpleFloodgateApi implements FloodgateApi {
    private final Map<UUID, FloodgatePlayer> players = new HashMap<>();
    private final PluginMessageHandler pluginMessageHandler;

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return getPlayer(uuid) != null;
    }

    @Override
    public FloodgatePlayer getPlayer(UUID uuid) {
        FloodgatePlayer player = players.get(uuid);
        // bedrock players are always stored by their xuid,
        // so we return the instance if we know that the given uuid is a Floodgate uuid
        if (player != null || isFloodgateId(uuid)) {
            return player;
        }

        // make it possible to find player by Java id (linked players)
        for (FloodgatePlayer player1 : players.values()) {
            if (player1.getCorrectUniqueId().equals(uuid)) {
                return player1;
            }
        }
        return null;
    }

    @Override
    public UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }

    @Override
    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return pluginMessageHandler.sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(UUID uuid, FormBuilder<?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    public FloodgatePlayer addPlayer(UUID uuid, FloodgatePlayer player) {
        return players.put(uuid, player);
    }

    /**
     * Removes a player (should only be used internally)
     *
     * @param onlineId    The UUID of the online player
     * @param removeLogin true if it should remove a sessions who is still logging in
     * @return the FloodgatePlayer the player was logged in with
     */
    @Nullable
    public FloodgatePlayer removePlayer(UUID onlineId, boolean removeLogin) {
        FloodgatePlayer selfPlayer = players.get(onlineId);
        // the player is a non-linked player or a linked player but somehow someone tried to
        // remove the player by his xuid, we have to find out
        if (selfPlayer != null) {
            // we don't allow them to remove a player by his xuid
            // because a linked player is never registered by his linked java uuid
            if (selfPlayer.getLinkedPlayer() != null) {
                return null;
            }

            // removeLogin logic
            if (!shouldRemove(selfPlayer, removeLogin)) {
                return null;
            }

            // passed the test
            players.remove(onlineId);
            // was the account linked?
            return selfPlayer;
        }

        // we still want to be able to remove a linked-player by his linked java uuid
        for (FloodgatePlayer player : players.values()) {
            if (shouldRemove(player, removeLogin) && player.getCorrectUniqueId().equals(onlineId)) {
                continue;
            }
            players.remove(player.getJavaUniqueId());
            return player;
        }

        return null;
    }

    protected boolean shouldRemove(FloodgatePlayer player, boolean removeLogin) {
        FloodgatePlayerImpl impl = player.as(FloodgatePlayerImpl.class);
        return impl.isLogin() && removeLogin || !impl.isLogin() && !removeLogin;
    }

    /**
     * Equivalant of {@link #removePlayer(UUID, boolean)} but with removeLogin = false.
     */
    public FloodgatePlayer removePlayer(UUID onlineId) {
        return removePlayer(onlineId, false);
    }

    /**
     * Equivalent of {@link #removePlayer(UUID, boolean)} except that it removes a FloodgatePlayer
     * instance directly.
     */
    public boolean removePlayer(FloodgatePlayer player) {
        boolean removed = players.remove(player.getJavaUniqueId(), player);
        return removed && player.getLinkedPlayer() != null;
    }
}
