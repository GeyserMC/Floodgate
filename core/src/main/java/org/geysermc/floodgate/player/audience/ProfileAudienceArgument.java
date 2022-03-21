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

package org.geysermc.floodgate.player.audience;

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
import org.geysermc.floodgate.platform.util.PlayerType;
import org.geysermc.floodgate.player.UserAudience;

public class ProfileAudienceArgument extends CommandArgument<UserAudience, ProfileAudience> {
    private ProfileAudienceArgument(@NonNull String name, ProfileAudienceParser parser) {
        super(true, name, parser, ProfileAudience.class);
    }

    public static ProfileAudienceArgument of(
            String name,
            boolean allowUuid,
            boolean allowOffline,
            PlayerType limitTo) {
        return new ProfileAudienceArgument(name,
                new ProfileAudienceParser(allowUuid, allowOffline, limitTo));
    }

    public static ProfileAudienceArgument of(
            String name,
            boolean allowOffline,
            PlayerType limitTo) {
        return of(name, false, allowOffline, limitTo);
    }

    public static ProfileAudienceArgument ofOnline(String name, PlayerType limitTo) {
        return of(name, false, false, limitTo);
    }

    public static ProfileAudienceArgument ofOnline(String name, boolean allowUuid) {
        return of(name, allowUuid, false, PlayerType.ALL_PLAYERS);
    }

    public static CommandArgument<UserAudience, ProfileAudience> ofOnline(String name) {
        return of(name, false, false, PlayerType.ALL_PLAYERS);
    }

    public static ProfileAudienceArgument of(String name, boolean allowOffline) {
        return of(name, false, allowOffline, PlayerType.ALL_PLAYERS);
    }

    @RequiredArgsConstructor
    public static final class ProfileAudienceParser
            implements ArgumentParser<UserAudience, ProfileAudience> {

        private final boolean allowUuid;
        private final boolean allowOffline;
        private final PlayerType limitTo;

        @Override
        public @NonNull ArgumentParseResult<ProfileAudience> parse(
                @NonNull CommandContext<@NonNull UserAudience> commandContext,
                @NonNull Queue<@NonNull String> inputQueue) {
            CommandUtil commandUtil = commandContext.get("CommandUtil");

            String input = inputQueue.poll();
            if (input == null || input.length() < 3) {
                return ArgumentParseResult.failure(
                        new NullPointerException("Expected player name/UUID"));
            }

            if (input.startsWith("\"")) {
                if (input.endsWith("\"")) {
                    // Remove quotes from both sides of this string
                    input = input.substring(1);
                    input = input.substring(0, input.length() - 1);
                } else {
                    // Multi-line
                    StringBuilder builder = new StringBuilder(input);
                    while (!inputQueue.isEmpty()) {
                        String string = inputQueue.remove();
                        builder.append(' ').append(string);
                        if (string.endsWith("\"")) {
                            break;
                        }
                    }

                    if (builder.lastIndexOf("\"") != builder.length() - 1) {
                        return ArgumentParseResult.failure(
                                new InvalidPlayerIdentifierException("Malformed string provided; " +
                                        "no end quotes found!"));
                    }

                    builder.deleteCharAt(0);
                    builder.deleteCharAt(builder.length() - 1);
                    input = builder.toString();
                }
            }

            ProfileAudience profileAudience;

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
                    Object player = commandUtil.getPlayerByUuid(UUID.fromString(input), limitTo);
                    profileAudience = commandUtil.getProfileAudience(player, allowOffline);
                } catch (final IllegalArgumentException ignored) {
                    return ArgumentParseResult.failure(
                            new InvalidPlayerIdentifierException("Invalid UUID '" + input + "'"));
                }
            } else {
                // This is a username.
                Object player = commandUtil.getPlayerByUsername(input, limitTo);
                profileAudience = commandUtil.getProfileAudience(player, allowOffline);
            }

            if (profileAudience == null) {
                return ArgumentParseResult.failure(
                        new InvalidPlayerIdentifierException("Invalid player '" + input + "'"));
            }

            return ArgumentParseResult.success(profileAudience);
        }

        @Override
        public @NonNull List<String> suggestions(
                @NonNull CommandContext<UserAudience> commandContext,
                @NonNull String input) {
            CommandUtil commandUtil = commandContext.get("CommandUtil");
            String trimmedInput = input.trim();

            if (trimmedInput.isEmpty()) {
                return ImmutableList.copyOf(commandUtil.getOnlineUsernames(limitTo));
            }

            String lowercaseInput = input.toLowerCase(Locale.ROOT);
            ImmutableList.Builder<String> builder = ImmutableList.builder();

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

        public InvalidPlayerIdentifierException(@NonNull String message) {
            super(message);
        }

        @Override
        public @NonNull Throwable fillInStackTrace() {
            return this;
        }
    }
}
