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

package org.geysermc.floodgate.player;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.CommandUtil;

public final class UserAudienceArgument extends CommandArgument<UserAudience, UserAudience> {
    private UserAudienceArgument(final @NonNull String name, final UserAudienceParser parser) {
        super(true, name, parser, UserAudience.class);
    }

    public static UserAudienceArgument of(
            final String name,
            final boolean allowUuid,
            final boolean allowOffline,
            final PlayerType limitTo) {
        return new UserAudienceArgument(name,
                new UserAudienceParser(allowUuid, allowOffline, limitTo));
    }

    public static UserAudienceArgument of(
            final String name,
            final boolean allowOffline,
            final PlayerType limitTo) {
        return of(name, false, allowOffline, limitTo);
    }

    public static UserAudienceArgument ofOnline(final String name, final PlayerType limitTo) {
        return of(name, false, false, limitTo);
    }

    public static UserAudienceArgument ofOnline(final String name, final boolean allowUuid) {
        return of(name, allowUuid, false, PlayerType.ALL_PLAYERS);
    }

    public static CommandArgument<UserAudience, UserAudience> ofOnline(final String name) {
        return of(name, false, false, PlayerType.ALL_PLAYERS);
    }

    public static UserAudienceArgument of(final String name, final boolean allowOffline) {
        return of(name, false, allowOffline, PlayerType.ALL_PLAYERS);
    }

    public enum PlayerType {
        ALL_PLAYERS,
        ONLY_BEDROCK,
        ONLY_JAVA
    }

    @RequiredArgsConstructor
    public static final class UserAudienceParser
            implements ArgumentParser<UserAudience, UserAudience> {

        private final boolean allowUuid;
        private final boolean allowOffline;
        private final PlayerType limitTo;

        @Override
        public @NonNull ArgumentParseResult<UserAudience> parse(
                final @NonNull CommandContext<@NonNull UserAudience> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue) {
            CommandUtil commandUtil = commandContext.get("CommandUtil");

            final String input = inputQueue.peek();
            if (input == null || input.length() < 3) {
                return ArgumentParseResult.failure(
                        new NullPointerException("Expected player name/UUID"));
            }

            UserAudience userAudience;

            if (input.length() > 16) {
                // This must be a UUID.
                if (!allowUuid) {
                    return ArgumentParseResult.failure(
                            new InvalidPlayerIdentifierException("UUID is not allowed here"));
                }

                if (input.length() != 32 && input.length() != 36) {
                    // Neither UUID without dashes nor with dashes.
                    return ArgumentParseResult.failure(
                            new InvalidPlayerIdentifierException("Expected player name/UUID"));
                }

                try {
                    // We only want to make sure the UUID is valid here.
                    final UUID uuid = UUID.fromString(input);
                    userAudience = commandUtil.getAudienceByUuid(uuid);

                    if (userAudience == null && allowOffline) {
                        userAudience = commandUtil.getOfflineAudienceByUuid(uuid);
                    }
                } catch (final IllegalArgumentException ignored) {
                    return ArgumentParseResult.failure(
                            new InvalidPlayerIdentifierException("Invalid UUID '" + input + "'"));
                }
            } else {
                // This is a username.
                userAudience = commandUtil.getAudienceByUsername(input);

                if (userAudience == null && allowOffline) {
                    userAudience = commandUtil.getOfflineAudienceByUsername(input);
                }
            }

            if (userAudience == null) {
                return ArgumentParseResult.failure(
                        new InvalidPlayerIdentifierException("Invalid player '" + input + "'"));
            }

            inputQueue.remove();
            return ArgumentParseResult.success(userAudience);
        }

        @Override
        public @NonNull List<String> suggestions(
                final @NonNull CommandContext<UserAudience> commandContext,
                final @NonNull String input) {
            final CommandUtil commandUtil = commandContext.get("CommandUtil");
            final String trimmedInput = input.trim();

            if (trimmedInput.isEmpty()) {
                return ImmutableList.copyOf(commandUtil.getOnlineUsernames(limitTo));
            }

            final String lowercaseInput = input.toLowerCase(Locale.ROOT);
            final ImmutableList.Builder<String> builder = ImmutableList.builder();

            for (final String player : commandUtil.getOnlineUsernames(limitTo)) {
                if (player.toLowerCase(Locale.ROOT).startsWith(lowercaseInput)) {
                    builder.add(player);
                }
            }

            return builder.build();
        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }

    public static final class InvalidPlayerIdentifierException extends IllegalArgumentException {
        private static final long serialVersionUID = -6500019324607183855L;

        public InvalidPlayerIdentifierException(final @NonNull String message) {
            super(message);
        }

        @Override
        public @NonNull Throwable fillInStackTrace() {
            return this;
        }
    }
}
