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

package org.geysermc.floodgate.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.connection.Connection;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.pluginmessage.channel.FormChannel;
import org.geysermc.floodgate.pluginmessage.channel.TransferChannel;
import org.geysermc.floodgate.util.HttpClient;

public class SimpleFloodgateApi implements GeyserApiBase {
    private final Map<UUID, Connection> players = new HashMap<>();
    private final Cache<UUID, Connection> pendingRemove =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .build();

    @Inject private PluginMessageManager pluginMessageManager;
    @Inject private FloodgateConfig config;
    @Inject private HttpClient httpClient;
    @Inject private FloodgateLogger logger;

    @Override
    public String usernamePrefix() {
        return config.getUsernamePrefix();
    }

    @Override
    public @NonNull List<? extends Connection> onlineConnections() {
        return players.values().stream().collect(Collectors.toList()); // TODO guarantee immutable
    }

    @Override
    public int onlineConnectionsCount() {
        return players.size();
    }

    @Override
    public boolean isBedrockPlayer(@NonNull UUID uuid) {
        return connectionByUuid(uuid) != null;
    }

    @Override
    public @Nullable Connection connectionByUuid(@NonNull UUID uuid) {
        Connection selfPlayer = players.get(uuid);
        if (selfPlayer != null) {
            return selfPlayer;
        }

        // bedrock players are always stored by their xuid,
        // so we return the instance if we know that the given uuid is a Floodgate uuid
        if (isFloodgateId(uuid)) {
            return pendingRemove.getIfPresent(uuid);
        }

        // make it possible to find player by Java id (linked players)
        // TODO still needed?
        for (Connection player : players.values()) {
            if (player.javaUuid().equals(uuid)) {
                return player;
            }
        }
        // and don't forget the pending remove linked players
        return getPendingRemovePlayer(uuid);
    }

    @Override
    public @Nullable Connection connectionByXuid(@NonNull String s) {
        return null;
    }

    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, @NonNull Form form) {
        return pluginMessageManager.getChannel(FormChannel.class).sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transfer(@NonNull UUID uuid, @NonNull String address, int port) {
        return pluginMessageManager
                .getChannel(TransferChannel.class)
                .sendTransfer(uuid, address, port);
    }

    /*
    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        if (gamertag == null || gamertag.isEmpty() || gamertag.length() > 16) {
            return Utils.failedFuture(new IllegalStateException("Received an invalid gamertag"));
        }

        return httpClient.asyncGet(Constants.GET_XUID_URL + gamertag)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();

                    if (!result.isCodeOk()) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonElement xuid = response.get("xuid");
                    return xuid != null ? xuid.getAsLong() : null;
                });
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return httpClient.asyncGet(Constants.GET_GAMERTAG_URL + xuid)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();

                    if (!result.isCodeOk()) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonElement gamertag = response.get("gamertag");
                    return gamertag != null ? gamertag.getAsString() : null;
                });
    }

    @Override
    public final Unsafe unsafe() {
        String callerClass = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.warn("A plugin is trying to access an unsafe part of the Floodgate api!" +
                " The use of this api can result in client crashes if used incorrectly." +
                " Caller: " + callerClass);
        return new UnsafeFloodgateApi(pluginMessageManager);
    }
    */

    public Connection addPlayer(Connection player) {
        // Bedrock players are always stored by their xuid
        return players.put(player.javaUuid(), player);
    }

    /**
     * This method is invoked when the player is no longer on the server, but the related platform-
     * dependant event hasn't fired yet
     * @param player
     */
    public boolean setPendingRemove(Connection player) {
        pendingRemove.put(player.javaUuid(), player);
        return players.remove(player.javaUuid(), player);
    }

    public void playerRemoved(UUID correctUuid) {
        // we can remove the player directly if it is a Floodgate UUID.
        // since it's stored by their Floodgate UUID
        if (isFloodgateId(correctUuid)) {
            pendingRemove.invalidate(correctUuid);
            return;
        }
        Connection linkedPlayer = getPendingRemovePlayer(correctUuid);
        if (linkedPlayer != null) {
            pendingRemove.invalidate(linkedPlayer.javaUuid());
        }
    }

    private Connection getPendingRemovePlayer(UUID correctUuid) {
        for (Connection player : pendingRemove.asMap().values()) {
            if (player.javaUuid().equals(correctUuid)) {
                return player;
            }
        }
        return null;
    }

    public PlayerLink getPlayerLink() { // TODO
        return InstanceHolder.getPlayerLink();
    }
}
