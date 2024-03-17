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

package org.geysermc.floodgate.core.command.main;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import com.google.gson.JsonElement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.command.CommonCommandMessage;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.HttpClient;
import org.incendo.cloud.context.CommandContext;

@Singleton
public class VersionSubcommand extends FloodgateSubCommand {
    @Inject HttpClient httpClient;
    @Inject FloodgateLogger logger;

    VersionSubcommand() {
        super(
                MainCommand.class,
                "version",
                "Displays version information about Floodgate",
                Permission.COMMAND_MAIN_VERSION
        );
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.sender();
        sender.sendMessage(
                Message.VERSION_INFO,
                literal("version", Constants.FULL_VERSION),
                literal("branch", Constants.GIT_BRANCH));
        sender.sendMessage(Message.VERSION_FETCH_INFO);

        String baseUrl = String.format(
                "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/%s/lastSuccessfulBuild/",
                Constants.GIT_BRANCH
        );

        httpClient.asyncGet(
                baseUrl + "buildNumber",
                JsonElement.class
        ).whenComplete((result, error) -> {
            if (error != null) {
                sender.sendMessage(Message.VERSION_FETCH_ERROR);
                error.printStackTrace();
                return;
            }

            JsonElement response = result.getResponse();

            logger.info(String.valueOf(response));
            logger.info("{}", result.getHttpCode());

            if (result.getHttpCode() == 404) {
                sender.sendMessage(Message.VERSION_FETCH_NOT_FOUND);
                return;
            }

            if (!result.isCodeOk()) {
                //todo make it more generic instead of using a Whitelist command strings
                logger.error(
                        "Got an error from requesting the latest Floodgate version: {}",
                        response.toString()
                );
                sender.sendMessage(CommonCommandMessage.UNEXPECTED_ERROR);
                return;
            }

            int buildNumber = response.getAsInt();

            if (buildNumber > Constants.BUILD_NUMBER) {
                sender.sendMessage(
                        Message.VERSION_OUTDATED,
                        literal("count", buildNumber - Constants.BUILD_NUMBER),
                        literal("url", baseUrl));
                return;
            }
            if (buildNumber == Constants.BUILD_NUMBER) {
                sender.sendMessage(Message.VERSION_LATEST);
                return;
            }
            sender.sendMessage(Message.VERSION_CUSTOM);
        });
    }

    public static final class Message {
        public static final TranslatableMessage VERSION_INFO = new TranslatableMessage("floodgate.command.main.version.info", MessageType.NORMAL);
        public static final TranslatableMessage VERSION_FETCH_INFO = new TranslatableMessage("floodgate.command.main.version.fetch.info", MessageType.INFO);
        public static final TranslatableMessage VERSION_FETCH_ERROR = new TranslatableMessage("floodgate.command.main.version.fetch.error", MessageType.ERROR);
        public static final TranslatableMessage VERSION_FETCH_NOT_FOUND = new TranslatableMessage("floodgate.command.main.version.fetch.not_found", MessageType.ERROR);
        public static final TranslatableMessage VERSION_OUTDATED = new TranslatableMessage("floodgate.command.main.version.fetch.result.outdated", MessageType.NORMAL);
        public static final TranslatableMessage VERSION_LATEST = new TranslatableMessage("floodgate.command.main.version.fetch.result.latest", MessageType.SUCCESS);
        public static final TranslatableMessage VERSION_CUSTOM = new TranslatableMessage("floodgate.command.main.version.fetch.result.custom", MessageType.ERROR);
    }
}
