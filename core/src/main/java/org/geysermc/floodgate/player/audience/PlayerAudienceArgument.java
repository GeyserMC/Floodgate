/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import static org.incendo.cloud.parser.standard.StringParser.quotedStringParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.util.PlayerType;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.util.BrigadierUtils;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.Suggestion;

public class PlayerAudienceArgument {

    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyIdentifierBedrock(String name) {
        return of(name, true, true, PlayerType.ONLY_BEDROCK);
    }

    public static CommandComponent.Builder<UserAudience, ProfileAudience> ofAnyUsernameBoth(String name) {
        return of(name, false, true, PlayerType.ALL_PLAYERS);
    }

    private static CommandComponent.Builder<UserAudience, ProfileAudience> of(String name, boolean allowUuid, boolean allowOffline, PlayerType limitTo) {
        return CommandComponent.<UserAudience, ProfileAudience>builder()
                .name(name)
                .parser(quotedStringParser().flatMapSuccess(ProfileAudience.class,
                        (context, input) -> {
                            CommandUtil commandUtil = context.get("CommandUtil");

                            ProfileAudience profileAudience;
                            if (input.length() > 16) {
                                // This must be a UUID.
                                if (!allowUuid) {
                                    return ArgumentParseResult.failureFuture(
                                            new InvalidPlayerIdentifierException(
                                                    "UUID is not allowed here"));
                                }

                                if (input.length() != 32 && input.length() != 36) {
                                    // Neither UUID without dashes nor with dashes.
                                    return ArgumentParseResult.failureFuture(
                                            new InvalidPlayerIdentifierException(
                                                    "Expected player name/UUID"));
                                }

                                try {
                                    // We only want to make sure the UUID is valid here.
                                    Object player = commandUtil.getPlayerByUuid(
                                            UUID.fromString(input), limitTo);
                                    profileAudience = commandUtil.getProfileAudience(player,
                                            allowOffline);
                                } catch (final IllegalArgumentException ignored) {
                                    return ArgumentParseResult.failureFuture(
                                            new InvalidPlayerIdentifierException(
                                                    "Invalid UUID '" + input + "'"));
                                }
                            } else {
                                // This is a username.
                                Object player = commandUtil.getPlayerByUsername(input, limitTo);
                                profileAudience = commandUtil.getProfileAudience(player,
                                        allowOffline);
                            }

                            if (profileAudience == null) {
                                return ArgumentParseResult.failureFuture(
                                        new InvalidPlayerIdentifierException(
                                                "Invalid player '" + input + "'"));
                            }

                            return ArgumentParseResult.successFuture(profileAudience);
                        }))
                .suggestionProvider((context, input) -> {
                    CommandUtil commandUtil = context.get("CommandUtil");

                    boolean quoted = input.remainingInput().startsWith("\"");
                    List<Suggestion> suggestions = new ArrayList<>();
                    for (final String player : commandUtil.getOnlineUsernames(limitTo)) {
                        suggestions.add(
                                Suggestion.suggestion(BrigadierUtils.escapeIfRequired(player, quoted)));
                    }
                    return CompletableFuture.completedFuture(suggestions);
                });
    }
}
