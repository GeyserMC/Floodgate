/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.pluginmessage.channel.FormChannel;
import org.geysermc.floodgate.pluginmessage.channel.TransferChannel;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpUtils;
import org.geysermc.floodgate.util.Utils;

@RequiredArgsConstructor
public class SimpleFloodgateApi implements FloodgateApi {
    private final Map<UUID, FloodgatePlayer> players = new HashMap<>();
    private final PluginMessageManager pluginMessageManager;
    private final FloodgateConfigHolder configHolder;

    @Override
    public String getPlayerPrefix() {
        return configHolder.get().getUsernamePrefix();
    }

    @Override
    public Collection<FloodgatePlayer> getPlayers() {
        return ImmutableSet.copyOf(players.values());
    }

    @Override
    public int getPlayerCount() {
        return players.size();
    }

    @Override
    public boolean isFloodgatePlayer(UUID uuid) {
        return getPlayer(uuid) != null;
    }

    @Override
    public FloodgatePlayer getPlayer(UUID uuid) {
        FloodgatePlayer selfPlayer = players.get(uuid);
        // bedrock players are always stored by their xuid,
        // so we return the instance if we know that the given uuid is a Floodgate uuid
        if (selfPlayer != null || isFloodgateId(uuid)) {
            return selfPlayer;
        }

        // make it possible to find player by Java id (linked players)
        for (FloodgatePlayer player : players.values()) {
            if (player.getCorrectUniqueId().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public UUID createJavaPlayerId(long xuid) {
        return Utils.getJavaUuid(xuid);
    }

    @Override
    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return pluginMessageManager.getChannel(FormChannel.class).sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(UUID uuid, FormBuilder<?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transferPlayer(UUID uuid, String address, int port) {
        return pluginMessageManager
                .getChannel(TransferChannel.class)
                .sendTransfer(uuid, address, port);
    }

    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        if (gamertag == null || gamertag.isEmpty() || gamertag.length() > 16) {
            return Utils.failedFuture(new IllegalStateException("Received an invalid gamertag"));
        }

        return HttpUtils.asyncGet(Constants.GET_XUID_URL + gamertag)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();
                    boolean success = response.get("success").getAsBoolean();

                    if (!success) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonObject data = response.getAsJsonObject("data");
                    if (data.size() == 0) {
                        return null;
                    }

                    return data.get("xuid").getAsLong();
                });
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return HttpUtils.asyncGet(Constants.GET_GAMERTAG_URL + xuid)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();
                    boolean success = response.get("success").getAsBoolean();

                    if (!success) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonObject data = response.getAsJsonObject("data");
                    if (data.size() == 0) {
                        return null;
                    }

                    return data.get("gamertag").getAsString();
                });
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
            if (!canRemove(selfPlayer, removeLogin)) {
                return null;
            }

            // passed the test
            players.remove(onlineId);
            // was the account linked?
            return selfPlayer;
        }

        // we still want to be able to remove a linked-player by his linked java uuid
        for (FloodgatePlayer player : players.values()) {
            if (canRemove(player, removeLogin) && player.getCorrectUniqueId().equals(onlineId)) {
                players.remove(player.getJavaUniqueId());
                return player;
            }
        }
        return null;
    }

    protected boolean canRemove(FloodgatePlayer player, boolean removeLogin) {
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
        return players.remove(player.getJavaUniqueId(), player);
    }
}
