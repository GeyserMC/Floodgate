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

package org.geysermc.floodgate.core.command;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.platform.command.FloodgateCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Utils;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

@Singleton
public class WhitelistCommand implements FloodgateCommand {
    @Inject FloodgateConfig config;
    @Inject XboxClient xboxClient;
    @Inject FloodgateLogger logger;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        Command.Builder<UserAudience> builder = commandManager.commandBuilder("fwhitelist",
                        Description.of("Easy way to whitelist Bedrock players"))
                .permission(Permission.COMMAND_WHITELIST.get());

        commandManager.command(builder
                .literal("add", "a")
                .argument(ProfileAudience.ofAnyIdentifierBedrock("player"))
                .handler(context -> performCommand(context, true)));

        return builder
                .literal("remove", "r")
                .argument(ProfileAudience.ofAnyIdentifierBedrock("player"))
                .handler(context -> performCommand(context, false))
                .build();
    }

    public void performCommand(CommandContext<UserAudience> context, boolean add) {
        UserAudience sender = context.sender();
        ProfileAudience profile = context.get("player");
        UUID uuid = profile.uuid();
        String name = profile.username();

        if (name == null && uuid == null) {
            sender.sendMessage(CommonCommandMessage.UNEXPECTED_ERROR);
            return;
        }

        if (uuid != null) {
            if (!Utils.isFloodgateUniqueId(uuid)) {
                sender.sendMessage(Message.INVALID_USERNAME);
                return;
            }

            CommandUtil commandUtil = context.get("CommandUtil");

            if (add) {
                if (commandUtil.whitelistPlayer(uuid, "unknown")) {
                    sender.sendMessage(Message.PLAYER_ADDED, literal("target", uuid.toString()));
                } else {
                    sender.sendMessage(Message.PLAYER_ALREADY_WHITELISTED, literal("target", uuid.toString()));
                }
            } else {
                if (commandUtil.removePlayerFromWhitelist(uuid, "unknown")) {
                    sender.sendMessage(Message.PLAYER_REMOVED, literal("target", uuid.toString()));
                } else {
                    sender.sendMessage(Message.PLAYER_NOT_WHITELISTED, literal("target", uuid.toString()));
                }
            }
            return;
        }

        if (name.startsWith(config.usernamePrefix())) {
            name = name.substring(config.usernamePrefix().length());
        }

        if (name.length() < 1 || name.length() > 16) {
            sender.sendMessage(Message.INVALID_USERNAME);
            return;
        }

        // todo let it use translations

        String tempName = name;
        if (config.replaceSpaces()) {
            tempName = tempName.replace(' ', '_');
        }
        final String correctName = config.usernamePrefix() + tempName;
        final String strippedName = name;

        // We need to get the UUID of the player if it's not manually specified
        xboxClient.xuidByGamertag(name)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        if (!(error instanceof HttpClientResponseException exception)) {
                            sender.sendMessage(Message.API_UNAVAILABLE);
                            error.printStackTrace();
                            return;
                        }
                        sender.sendMessage(CommonCommandMessage.UNEXPECTED_ERROR);

                        //todo proper non-200 status handler
//                        var response = exception.getResponse().getBody(UnsuccessfulResponse.class);
//                        var message =
//                                response.isPresent() ?
//                                        response.get().message() :
//                                        exception.getMessage();
//                        logger.error(
//                                "Got an error from requesting the xuid of a Bedrock player: {}",
//                                message
//                        );
                        return;
                    }

                    Long xuid = result.xuid();
                    if (xuid == null) {
                        sender.sendMessage(Message.ACCOUNT_NOT_FOUND);
                        return;
                    }

                    CommandUtil commandUtil = context.get("CommandUtil");

                    try {
                        if (add) {
                            if (commandUtil.whitelistPlayer(xuid, correctName)) {
                                sender.sendMessage(Message.PLAYER_ADDED, literal("target", strippedName));
                            } else {
                                sender.sendMessage(Message.PLAYER_ALREADY_WHITELISTED, literal("target", strippedName));
                            }
                        } else {
                            if (commandUtil.removePlayerFromWhitelist(xuid, correctName)) {
                                sender.sendMessage(Message.PLAYER_REMOVED, literal("target", strippedName));
                            } else {
                                sender.sendMessage(Message.PLAYER_NOT_WHITELISTED, literal("target", strippedName));
                            }
                        }
                    } catch (Exception exception) {
                        logger.error(
                                "An unexpected error happened while executing the whitelist command",
                                exception
                        );
                    }
                });
    }

    @Override
    public boolean shouldRegister(FloodgateConfig config) {
        // currently only Spigot (our only non-Proxy platform) has a whitelist build-in.
        return !(config instanceof ProxyFloodgateConfig);
    }

    public static final class Message {
        public static final TranslatableMessage INVALID_USERNAME = new TranslatableMessage("floodgate.command.fwhitelist.invalid_username", MessageType.ERROR);
        public static final TranslatableMessage API_UNAVAILABLE = new TranslatableMessage("floodgate.command.fwhitelist.api_unavailable " + CommonCommandMessage.CHECK_CONSOLE, MessageType.ERROR);
        public static final TranslatableMessage ACCOUNT_NOT_FOUND = new TranslatableMessage("floodgate.command.fwhitelist.account_not_found", MessageType.ERROR);
        public static final TranslatableMessage PLAYER_ADDED = new TranslatableMessage("floodgate.command.fwhitelist.player_added", MessageType.SUCCESS);
        public static final TranslatableMessage PLAYER_REMOVED = new TranslatableMessage("floodgate.command.fwhitelist.player_removed", MessageType.SUCCESS);
        public static final TranslatableMessage PLAYER_ALREADY_WHITELISTED = new TranslatableMessage("floodgate.command.fwhitelist.player_already_whitelisted", MessageType.NORMAL);
        public static final TranslatableMessage PLAYER_NOT_WHITELISTED = new TranslatableMessage("floodgate.command.fwhitelist.player_not_whitelisted", MessageType.NORMAL);
    }
}
