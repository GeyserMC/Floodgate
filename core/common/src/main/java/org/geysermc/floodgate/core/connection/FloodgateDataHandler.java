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

package org.geysermc.floodgate.core.connection;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.geysermc.floodgate.api.event.FloodgateEventBus;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.crypto.FloodgateDataCodec;
import org.geysermc.floodgate.core.crypto.exception.UnsupportedVersionException;
import org.geysermc.floodgate.core.event.ConnectionJoinEvent;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.CommonPlatformMessages;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.InvalidFormatException;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.core.util.Utils;
import org.geysermc.floodgate.util.LinkedPlayer;

@Singleton
public final class FloodgateDataHandler {
    @Inject ConnectionManager connectionManager;
    @Inject CommonPlayerLink link;
    @Inject FloodgateConfig config;
    @Inject FloodgateLogger logger;
    @Inject LanguageManager languageManager;
    @Inject FloodgateEventBus eventBus;
    @Inject FloodgateDataCodec dataCodec;

    /**
     * Decode the given Floodgate data into a FloodgateConnection.
     * Throws {@link InvalidFormatException} when the Floodgate format couldn't be found.
     * Throws {@link UnsupportedVersionException} when the given format version isn't supported.
     * Other {@link Exception}(s) are caused by invalid/tempered Floodgate data.
     *
     * @param floodgateData the data to decode
     * @return the Floodgate Connection if decoded successfully
     */
    @SneakyThrows
    public FloodgateConnection decodeDataToConnection(@NonNull String floodgateData) {
        return dataCodec.decodeFromString(floodgateData);
    }

    public CompletableFuture<JoinResult> handleConnection(FloodgateConnection connection) {
        return handleLink(connection)
                .thenApplyAsync(this::canJoin)
                .thenApply(result -> {
                    if (!result.shouldDisconnect()) {
                        connectionManager.addConnection(connection);
                    }
                    return result;
                });
    }

    private CompletableFuture<FloodgateConnection> handleLink(FloodgateConnection connection) {
        return maybeFetchLink(connection).thenApply(linkedPlayer -> {
            if (connection.isLinked() && linkedPlayer == null) {
                return connection;
            }
            return connection.linkedPlayer(linkedPlayer);
        });
    }

    private JoinResult canJoin(FloodgateConnection connection) {
        String disconnectReason = null;
        if (config.playerLink().requireLink() && !connection.isLinked()) {
            disconnectReason = MiniMessage.miniMessage().serialize(
                    CommonPlatformMessages.NOT_LINKED.translateMessage(
                            languageManager,
                            connection.languageCode(),
                            literal("url", Constants.LINK_INFO_URL)));
        }

        var event = new ConnectionJoinEvent(connection, disconnectReason);
        // fire itself doesn't throw exceptions, only logs them
        eventBus.fire(event);

        return new JoinResult(connection, event.disconnectReason());
    }

    public record JoinResult(FloodgateConnection connection, @MonotonicNonNull String disconnectReason) {
        public boolean shouldDisconnect() {
            return disconnectReason != null;
        }
    }

    public enum HandleResultType {
        NOT_FLOODGATE_DATA,
        DECRYPT_ERROR,
        INVALID_DATA, //todo remove from config, unused
        SUCCESS
    }

    public record HandleResult(
            HandleResultType type,
            @EnsuresNonNullIf(expression = "type == HandleResultType.SUCCESS", result = true)
            JoinResult joinResult
    ) {}

    private CompletableFuture<LinkedPlayer> maybeFetchLink(FloodgateConnection connection) {
        // only fetch link if they're not already linked & linking is enabled
        if (!link.isEnabled() || connection.isLinked()) {
            return CompletableFuture.completedFuture(null);
        }
        return link.fetchLink(Utils.toFloodgateUniqueId(connection.xuid()))
                .thenApply(link -> {
                    if (link == null) {
                        return null;
                    }
                    return LinkedPlayer.of(link.javaUsername(), link.javaUniqueId(), link.bedrockId());
                })
                .exceptionally(error -> {
                    logger.error("The player linking implementation returned an error", error.getCause());
                    return null;
                });
    }

}
