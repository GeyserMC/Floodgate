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

package org.geysermc.floodgate.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.Description;
import cloud.commandframework.context.CommandContext;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudienceArgument;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpUtils;

public class WhitelistCommand implements FloodgateCommand {
    @Inject private FloodgateConfig config;
    @Inject private FloodgateLogger logger;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        Command.Builder<UserAudience> builder = commandManager.commandBuilder("fwhitelist",
                Description.of("Easy way to whitelist Bedrock players"))
                .permission("floodgate.command.fwhitelist");

        commandManager.command(builder
                .literal("add", "a")
                .argument(UserAudienceArgument.of("player", true))
                .handler(context -> performCommand(context, true)));

        return builder
                .literal("remove", "r")
                .argument(UserAudienceArgument.of("player", true))
                .handler(context -> performCommand(context, false))
                .build();
    }

    public void performCommand(CommandContext<UserAudience> context, boolean add) {
        UserAudience sender = context.getSender();
        UserAudience player = context.get("player");
        String name = player.username();

        if (name.startsWith(config.getUsernamePrefix())) {
            name = name.substring(config.getUsernamePrefix().length());
        }

        if (name.length() < 1 || name.length() > 16) {
            sender.sendMessage(Component.text(
                    "The given username '" + name + "' is not a valid username."));
            return;
        }

        // todo let it use translations

        final String tempName = name;

        HttpUtils.asyncGet(Constants.GET_XUID_URL + name)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        sender.sendMessage(Component.text(
                                "An error occurred. See the console for more info"));
                        error.printStackTrace();
                        return;
                    }

                    JsonObject response = result.getResponse();
                    boolean success = response.get("success").getAsBoolean();

                    if (!success) {
                        sender.sendMessage(Component.text(
                                "An error occurred. See the console for more info"));
                        logger.error(
                                "Got an error from requesting the xuid of a Bedrock player: {}",
                                response.get("message").getAsString());
                        return;
                    }

                    JsonObject data = response.getAsJsonObject("data");
                    if (data.size() == 0) {
                        sender.sendMessage(Component.text(
                                "Couldn't find the user '" + tempName + "'"));
                        return;
                    }

                    String xuid = data.get("xuid").getAsString();
                    CommandUtil commandUtil = context.get("CommandUtil");

                    try {
                        if (add && commandUtil.whitelistPlayer(xuid, tempName)) {
                            sender.sendMessage(Component.text("Player has been whitelisted :)"));
                        } else if (!add && commandUtil.removePlayerFromWhitelist(xuid, tempName)) {
                            sender.sendMessage(Component.text("Player has been removed :o"));
                        } else {
                            sender.sendMessage(Component.text("Player was already whitelisted :o"));
                        }
                    } catch (Exception exception) {
                        logger.error(
                                "An unexpected error happened while executing the whitelist command",
                                exception);
                    }
                });
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        // ignored, all the logic is in the other method
    }
}
