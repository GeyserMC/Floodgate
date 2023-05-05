/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import io.micronaut.context.BeanProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.connection.Connection;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.core.pluginmessage.channel.FormChannel;
import org.geysermc.floodgate.core.pluginmessage.channel.TransferChannel;
import org.geysermc.floodgate.core.scope.ServerOnly;

@ServerOnly
@Singleton
public class SimpleFloodgateApi implements GeyserApiBase {
    private final Map<UUID, Connection> players = new ConcurrentHashMap<>();
    private final Cache<UUID, Connection> pendingRemove =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .build();

    @Inject BeanProvider<PluginMessageManager> pluginMessageManager;
    @Inject FloodgateConfig config;
    @Inject FloodgateLogger logger;
    @Inject XboxClient xboxClient;

    @Override
    public String usernamePrefix() {
        return config.usernamePrefix();
    }

    @Override
    public @NonNull List<? extends Connection> onlineConnections() {
        return ImmutableList.copyOf(players.values());
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
        return pluginMessageManager.get().getChannel(FormChannel.class).sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transfer(@NonNull UUID uuid, @NonNull String address, int port) {
        return pluginMessageManager.get()
                .getChannel(TransferChannel.class)
                .sendTransfer(uuid, address, port);
    }

    /*
    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        return xboxClient.xuidByGamertag(gamertag).thenApply(GetXuidResult::xuid);
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return xboxClient.gamertagByXuid(xuid).thenApply(GetGamertagResult::gamertag);
    }

    @Override
    public final Unsafe unsafe() {
        String callerClass = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.warn("A plugin is trying to access an unsafe part of the Floodgate api!" +
                " The use of this api can result in client crashes if used incorrectly." +
                " Caller: " + callerClass);
        return new UnsafeFloodgateApi(pluginMessageManager.get());
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
