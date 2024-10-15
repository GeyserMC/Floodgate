/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.connection;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.jodah.expiringmap.ExpiringMap;
import net.kyori.adventure.text.Component;
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
    @Inject
    ConnectionManager connectionManager;

    @Inject
    CommonPlayerLink link;

    @Inject
    FloodgateConfig config;

    @Inject
    FloodgateLogger logger;

    @Inject
    LanguageManager languageManager;

    @Inject
    FloodgateEventBus eventBus;

    @Inject
    FloodgateDataCodec dataCodec;

    ExpiringMap<String, Boolean> nextLoginIsLinkRequest;

    @PostConstruct
    void init() {
        var playerLink = config.playerLink();
        if (playerLink.enabled()
                && playerLink.enableOwnLinking()
                && playerLink.requireLink()
                && playerLink.allowCreateLinkRequest()) {
            nextLoginIsLinkRequest =
                    ExpiringMap.builder().expiration(60, TimeUnit.SECONDS).build();
        }
    }

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
        return handleLink(connection).thenCompose(this::canJoin).thenApply(result -> {
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

    private CompletableFuture<JoinResult> canJoin(FloodgateConnection connection) {
        // todo add a @Requires annotation that requires a specific node to be true in order for the node to not be
        // false
        if (config.playerLink().enabled() && config.playerLink().requireLink() && !connection.isLinked()) {
            if (config.playerLink().allowCreateLinkRequest()) {
                var alreadyPresent = nextLoginIsLinkRequest.putIfAbsent(connection.xuid(), true) != null;

                if (alreadyPresent) {
                    return link.createBedrockLinkRequest(
                                    Utils.toFloodgateUniqueId(connection.xuid()), connection.bedrockUsername())
                            .handle((code, error) -> {
                                if (error != null) {
                                    logger.error("Could not create link request!", error);
                                    return canJoin(connection, null);
                                }

                                System.out.println(code + " " + code.getClass());
                                return canJoin(
                                        connection,
                                        Component.text("Your link code is %s, run %s to link with your Java account"
                                                .formatted(
                                                        code,
                                                        "/linkaccount %s %s"
                                                                .formatted(connection.bedrockUsername(), code))));
                            });
                } else {
                    return CompletableFuture.completedFuture(canJoin(
                            connection,
                            Component.text("You can also create a new link request by joining the server once more")));
                }
            }
        }
        return CompletableFuture.completedFuture(canJoin(connection, null));
    }

    private JoinResult canJoin(FloodgateConnection connection, Component linkMessage) {
        Component disconnectReason = null;
        if (config.playerLink().enabled() && config.playerLink().requireLink() && !connection.isLinked()) {
            disconnectReason = CommonPlatformMessages.NOT_LINKED.translateMessage(
                    languageManager, connection.languageCode(), literal("url", Constants.LINK_INFO_URL));

            if (linkMessage != null) {
                disconnectReason =
                        disconnectReason.appendNewline().appendNewline().append(linkMessage);
            }
        }

        var event = new ConnectionJoinEvent(connection, disconnectReason);
        // fire itself doesn't throw exceptions, only logs them
        eventBus.fire(event);

        return new JoinResult(connection, event.disconnectReason());
    }

    public record JoinResult(FloodgateConnection connection, @MonotonicNonNull Component disconnectReason) {
        public boolean shouldDisconnect() {
            return disconnectReason != null;
        }
    }

    public enum HandleResultType {
        NOT_FLOODGATE_DATA,
        DECRYPT_ERROR,
        INVALID_DATA, // todo remove from config, unused
        SUCCESS
    }

    public record HandleResult(
            HandleResultType type,
            @EnsuresNonNullIf(expression = "type == HandleResultType.SUCCESS", result = true) JoinResult joinResult) {}

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
