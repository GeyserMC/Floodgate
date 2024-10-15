/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.connection.audience;

import static org.incendo.cloud.parser.standard.StringParser.quotedStringParser;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.platform.util.PlayerType;
import org.geysermc.floodgate.core.util.BrigadierUtils;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.Suggestion;

public record ProfileAudience(@Nullable UUID uuid, @Nullable String username) {
    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyIdentifierBedrock(String name) {
        return of(name, true, true, PlayerType.ONLY_BEDROCK);
    }

    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyIdentifierJava(String name) {
        return of(name, true, true, PlayerType.ONLY_JAVA);
    }

    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyIdentifierBoth(String name) {
        return of(name, true, true, PlayerType.ALL_PLAYERS);
    }

    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyUsernameBoth(String name) {
        return of(name, false, true, PlayerType.ALL_PLAYERS);
    }

    private static CommandComponent.Builder<UserAudience, ProfileAudience> of(
            String name, boolean allowUuid, boolean allowOffline, PlayerType limitTo) {
        return CommandComponent.<UserAudience, ProfileAudience>builder()
                .name(name)
                .parser(quotedStringParser().flatMapSuccess(ProfileAudience.class, (context, input) -> {
                    CommandUtil commandUtil = context.get("CommandUtil");

                    ProfileAudience profileAudience;
                    if (input.length() > 16) {
                        // This must be a UUID.
                        if (!allowUuid) {
                            return ArgumentParseResult.failureFuture(
                                    new InvalidPlayerIdentifierException("UUID is not allowed here"));
                        }

                        if (input.length() != 32 && input.length() != 36) {
                            // Neither UUID without dashes nor with dashes.
                            return ArgumentParseResult.failureFuture(
                                    new InvalidPlayerIdentifierException("Expected player name/UUID"));
                        }

                        try {
                            // We only want to make sure the UUID is valid here.
                            Object player = commandUtil.getPlayerByUuid(UUID.fromString(input), limitTo);
                            profileAudience = commandUtil.getProfileAudience(player, allowOffline);
                        } catch (final IllegalArgumentException ignored) {
                            return ArgumentParseResult.failureFuture(
                                    new InvalidPlayerIdentifierException("Invalid UUID '" + input + "'"));
                        }
                    } else {
                        // This is a username.
                        Object player = commandUtil.getPlayerByUsername(input, limitTo);
                        profileAudience = commandUtil.getProfileAudience(player, allowOffline);
                    }

                    if (profileAudience == null) {
                        return ArgumentParseResult.failureFuture(
                                new InvalidPlayerIdentifierException("Invalid player '" + input + "'"));
                    }

                    return ArgumentParseResult.successFuture(profileAudience);
                }))
                .suggestionProvider((context, input) -> {
                    CommandUtil commandUtil = context.get("CommandUtil");

                    var quoted = input.remainingInput().startsWith("\"");

                    var suggestions = new ArrayList<Suggestion>();
                    for (final String player : commandUtil.getOnlineUsernames(limitTo)) {
                        suggestions.add(Suggestion.suggestion(BrigadierUtils.escapeIfRequired(player, quoted)));
                    }

                    return CompletableFuture.completedFuture(suggestions);
                });
    }
}
