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

package org.geysermc.floodgate.command.main;

import static org.geysermc.floodgate.util.Constants.COLOR_CHAR;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.command.WhitelistCommand.Message;
import org.geysermc.floodgate.command.util.Permission;
import org.geysermc.floodgate.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpClient;
import org.incendo.cloud.context.CommandContext;

public class VersionSubcommand extends FloodgateSubCommand {
    @Inject
    private HttpClient httpClient;

    @Inject
    private FloodgateLogger logger;

    @Inject
    @Named("implementationName")
    private String implementationName;

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Displays version information about Floodgate";
    }

    @Override
    public Permission permission() {
        return Permission.COMMAND_MAIN_VERSION;
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.sender();
        sender.sendMessage(String.format(
                COLOR_CHAR + "7You're currently on " + COLOR_CHAR + "b%s" +
                COLOR_CHAR + "7 (branch: " + COLOR_CHAR + "b%s" + COLOR_CHAR + "7)\n" +
                COLOR_CHAR + "eFetching latest build info...",
                Constants.VERSION, Constants.GIT_BRANCH
        ));

        //noinspection ConstantValue
        if (!Constants.GIT_MAIN_BRANCH.equals(Constants.GIT_BRANCH)) {
            sender.sendMessage(String.format(
                    COLOR_CHAR + "7Detected that you aren't on the %s branch, " +
                    "so we can't fetch the latest version.",
                    Constants.GIT_MAIN_BRANCH
            ));
            return;
        }

        httpClient.asyncGet(
                String.format(Constants.LATEST_VERSION_URL, Constants.PROJECT_NAME),
                JsonObject.class
        ).whenComplete((result, error) -> {
            if (error != null) {
                sender.sendMessage(COLOR_CHAR + "cCould not retrieve latest version info!");
                error.printStackTrace();
                return;
            }

            JsonObject response = result.getResponse();

            logger.debug("code: {}, response: ", result.getHttpCode(), String.valueOf(response));

            if (result.getHttpCode() == 404) {
                sender.sendMessage(
                        COLOR_CHAR + "cGot a 404 (not found) while requesting the latest version." +
                        " Are you using a custom Floodgate version?"
                );
                return;
            }

            if (!result.isCodeOk()) {
                //todo make it more generic instead of using a Whitelist command strings
                logger.error(
                        "Got an error from requesting the latest Floodgate version: {}",
                        String.valueOf(response)
                );
                sender.sendMessage(Message.UNEXPECTED_ERROR);
                return;
            }

            int buildNumber = response.get("build").getAsInt();

            if (buildNumber > Constants.BUILD_NUMBER) {
                sender.sendMessage(String.format(
                        COLOR_CHAR + "7There is a newer version of Floodgate available!\n" +
                        COLOR_CHAR + "7You are " + COLOR_CHAR + "e%s " + COLOR_CHAR + "7builds behind.\n" +
                        COLOR_CHAR + "7Download the latest Floodgate version here: " + COLOR_CHAR + "b%s",
                        buildNumber - Constants.BUILD_NUMBER,
                        String.format(Constants.LATEST_DOWNLOAD_URL, implementationName)
                ));
                return;
            }
            if (buildNumber == Constants.BUILD_NUMBER) {
                sender.sendMessage(COLOR_CHAR + "aYou're running the latest version of Floodgate!");
                return;
            }
            sender.sendMessage(COLOR_CHAR + "cCannot check version for custom Floodgate versions!");
        });
    }
}
