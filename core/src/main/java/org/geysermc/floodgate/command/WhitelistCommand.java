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

package org.geysermc.floodgate.command;

import static org.geysermc.floodgate.command.CommonCommandMessage.CHECK_CONSOLE;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import java.util.UUID;
import lombok.Getter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.command.util.Permission;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.platform.command.TranslatableMessage;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.audience.PlayerAudienceArgument;
import org.geysermc.floodgate.player.audience.ProfileAudience;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpClient;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

public class WhitelistCommand implements FloodgateCommand {
    @Inject private FloodgateConfig config;
    @Inject private HttpClient httpClient;
    @Inject private FloodgateLogger logger;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        Command.Builder<UserAudience> builder = commandManager.commandBuilder("fwhitelist",
                Description.of("Easy way to whitelist Bedrock players"))
                .permission(Permission.COMMAND_WHITELIST.get());

        commandManager.command(builder
                .literal("add", "a")
                .argument(PlayerAudienceArgument.ofAnyIdentifierBedrock("player"))
                .handler(context -> performCommand(context, true)));

        return builder
                .literal("remove", "r")
                .argument(PlayerAudienceArgument.ofAnyIdentifierBedrock("player"))
                .handler(context -> performCommand(context, false))
                .build();
    }

    public void performCommand(CommandContext<UserAudience> context, boolean add) {
        UserAudience sender = context.sender();
        ProfileAudience profile = context.get("player");
        UUID uuid = profile.uuid();
        String name = profile.username();

        if (name == null && uuid == null) {
            sender.sendMessage(Message.UNEXPECTED_ERROR);
            return;
        }

        if (uuid != null) {
            if (!FloodgateApi.getInstance().isFloodgateId(uuid)) {
                sender.sendMessage(Message.INVALID_USERNAME);
                return;
            }

            CommandUtil commandUtil = context.get("CommandUtil");

            if (add) {
                if (commandUtil.whitelistPlayer(uuid, "unknown")) {
                    sender.sendMessage(Message.PLAYER_ADDED, uuid.toString());
                } else {
                    sender.sendMessage(Message.PLAYER_ALREADY_WHITELISTED,
                            uuid.toString());
                }
            } else {
                if (commandUtil.removePlayerFromWhitelist(uuid, "unknown")) {
                    sender.sendMessage(Message.PLAYER_REMOVED, uuid.toString());
                } else {
                    sender.sendMessage(Message.PLAYER_NOT_WHITELISTED, uuid.toString());
                }
            }
            return;
        }

        if (name.startsWith(config.getUsernamePrefix())) {
            name = name.substring(config.getUsernamePrefix().length());
        }

        if (name.isEmpty() || name.length() > 16) {
            sender.sendMessage(Message.INVALID_USERNAME);
            return;
        }

        // todo let it use translations

        String tempName = name;
        if (config.isReplaceSpaces()) {
            tempName = tempName.replace(' ', '_');
        }
        final String correctName = config.getUsernamePrefix() + tempName;
        final String strippedName = name;

        // We need to get the UUID of the player if it's not manually specified
        httpClient.asyncGet(Constants.GET_XUID_URL + name)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        sender.sendMessage(Message.API_UNAVAILABLE);
                        error.printStackTrace();
                        return;
                    }

                    JsonObject response = result.getResponse();

                    if (!result.isCodeOk()) {
                        sender.sendMessage(Message.UNEXPECTED_ERROR);
                        logger.error(
                                "Got an error from requesting the xuid of a Bedrock player: {}",
                                response.get("message").getAsString()
                        );
                    }

                    JsonElement xuidElement = response.get("xuid");

                    if (xuidElement == null) {
                        sender.sendMessage(Message.USER_NOT_FOUND);
                        return;
                    }

                    String xuid = xuidElement.getAsString();
                    CommandUtil commandUtil = context.get("CommandUtil");

                    try {
                        if (add) {
                            if (commandUtil.whitelistPlayer(xuid, correctName)) {
                                sender.sendMessage(Message.PLAYER_ADDED, strippedName);
                            } else {
                                sender.sendMessage(Message.PLAYER_ALREADY_WHITELISTED,
                                        strippedName);
                            }
                        } else {
                            if (commandUtil.removePlayerFromWhitelist(xuid, correctName)) {
                                sender.sendMessage(Message.PLAYER_REMOVED, strippedName);
                            } else {
                                sender.sendMessage(Message.PLAYER_NOT_WHITELISTED, strippedName);
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

    @Getter
    public enum Message implements TranslatableMessage {
        INVALID_USERNAME("floodgate.command.fwhitelist.invalid_username"),
        API_UNAVAILABLE("floodgate.command.fwhitelist.api_unavailable " + CHECK_CONSOLE),
        USER_NOT_FOUND("floodgate.command.fwhitelist.user_not_found"),
        PLAYER_ADDED("floodgate.command.fwhitelist.player_added"),
        PLAYER_REMOVED("floodgate.command.fwhitelist.player_removed"),
        PLAYER_ALREADY_WHITELISTED("floodgate.command.fwhitelist.player_already_whitelisted"),
        PLAYER_NOT_WHITELISTED("floodgate.command.fwhitelist.player_not_whitelisted"),
        UNEXPECTED_ERROR("floodgate.command.fwhitelist.unexpected_error " + CHECK_CONSOLE);

        private final String rawMessage;
        private final String[] translateParts;

        Message(String rawMessage) {
            this.rawMessage = rawMessage;
            this.translateParts = rawMessage.split(" ");
        }
    }
}
