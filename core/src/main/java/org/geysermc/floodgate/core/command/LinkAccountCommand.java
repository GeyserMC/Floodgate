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
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience.PlayerAudience;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.link.LinkVerificationException;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.FloodgateCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Constants;
import org.geysermc.floodgate.core.util.Utils;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

@Singleton
public final class LinkAccountCommand implements FloodgateCommand {
    @Inject SimpleFloodgateApi api;
    @Inject CommonPlayerLink link;
    @Inject FloodgateLogger logger;

    @Override
    public Command<PlayerAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager.commandBuilder("linkaccount",
                Description.of("Link your Java account with your Bedrock account"))
                .senderType(PlayerAudience.class)
                .permission(Permission.COMMAND_LINK.get())
                .argument(ProfileAudience.ofAnyUsernameBoth("player"))
                .optional("code", stringParser())
                .handler(this::execute)
                .build();
    }

    public void execute(CommandContext<PlayerAudience> context) {
        UserAudience sender = context.sender();

        var linkState = link.state();
        if (!linkState.localLinkingActive()) {
            if (!linkState.globalLinkingEnabled()) {
                sender.sendMessage(CommonCommandMessage.LINKING_DISABLED);
            } else {
                sender.sendMessage(
                        CommonCommandMessage.GLOBAL_LINKING_NOTICE,
                        literal("url", Constants.LINK_INFO_URL));
            }
            return;
        } else if (linkState.globalLinkingEnabled()) {
            sender.sendMessage(
                    CommonCommandMessage.LOCAL_LINKING_NOTICE,
                    literal("url", Constants.LINK_INFO_URL));
        }

        ProfileAudience targetUser = context.get("player");
        // allowUuid is false so username cannot be null
        String targetName = targetUser.username();

        // when the player is a Bedrock player
        if (api.isBedrockPlayer(sender.uuid())) {
            if (!context.contains("code")) {
                sender.sendMessage(Message.BEDROCK_USAGE, literal("command", "/linkaccount <gamertag>"));
                return;
            }

            String code = context.get("code");

            link.linkRequest(targetName)
                    .thenApply(request -> {
                        if (request == null || link.isRequestedPlayer(request, sender.uuid())) {
                            throw LinkVerificationException.NO_LINK_REQUESTED;
                        }

                        if (!request.linkCode().equals(code)) {
                            throw LinkVerificationException.INVALID_CODE;
                        }

                        if (request.isExpired(link.getVerifyLinkTimeout())) {
                            throw LinkVerificationException.LINK_REQUEST_EXPIRED;
                        }

                        return request;
                    })
                    .thenCompose(request ->
                            CompletableFuture.allOf(
                                    link.invalidateLinkRequest(request),
                                    link.addLink(
                                            request.javaUniqueId(), request.javaUsername(), sender.uuid()
                                    )
                            )
                    )
                    .whenComplete(($, throwable) -> {
                        if (throwable instanceof LinkVerificationException exception) {
                            sender.sendMessage(exception.message(), exception.placeholders());
                            return;
                        }
                        if (throwable != null) {
                            sender.sendMessage(Message.LINK_REQUEST_ERROR);
                            return;
                        }
                        sender.disconnect(
                                Message.LINK_REQUEST_COMPLETED,
                                literal("target", targetName),
                                literal("command", "/unlinkaccount"));
                    });
            return;
        }

        if (context.contains("code")) {
            sender.sendMessage(Message.JAVA_USAGE, literal("command", "/linkaccount <gamertag>"));
            return;
        }

        String username = sender.username();
        String code = Utils.generateCode(6);

        link.createLinkRequest(sender.uuid(), username, targetName, code)
                .whenComplete(($, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(Message.LINK_REQUEST_ERROR);
                        return;
                    }
                    sender.sendMessage(
                            Message.LINK_REQUEST_CREATED,
                            literal("target", targetName),
                            literal("command", "/linkaccount " + username + " " + code),
                            literal("unlink_command", "/unlinkaccount"));
                });
    }

    @Override
    public boolean shouldRegister(FloodgateConfig config) {
        FloodgateConfig.PlayerLinkConfig linkConfig = config.playerLink();
        return linkConfig.enabled() &&
                (linkConfig.enableOwnLinking() || linkConfig.enableGlobalLinking());
    }

    public static final class Message {
        public static final TranslatableMessage ALREADY_LINKED = new TranslatableMessage("floodgate.command.link_account.already_linked", MessageType.ERROR);
        public static final TranslatableMessage JAVA_USAGE = new TranslatableMessage("floodgate.command.link_account.java_usage", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_CREATED = new TranslatableMessage("floodgate.command.link_account.link_request_created", MessageType.SUCCESS);
        public static final TranslatableMessage BEDROCK_USAGE = new TranslatableMessage("floodgate.command.link_account.bedrock_usage", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_EXPIRED = new TranslatableMessage("floodgate.command.link_account.link_request_expired", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_COMPLETED = new TranslatableMessage("floodgate.command.link_account.link_request_completed", MessageType.SUCCESS);
        public static final TranslatableMessage LINK_REQUEST_ERROR = new TranslatableMessage("floodgate.command.link_account.link_request_error " + CommonCommandMessage.CHECK_CONSOLE, MessageType.ERROR);
        public static final TranslatableMessage INVALID_CODE = new TranslatableMessage("floodgate.command.link_account.invalid_code", MessageType.ERROR);
        public static final TranslatableMessage NO_LINK_REQUESTED = new TranslatableMessage("floodgate.command.link_account.no_link_requested", MessageType.ERROR);
    }
}
