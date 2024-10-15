/*
 * Copyright (c) 2019-2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.command;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience.PlayerAudience;
import org.geysermc.floodgate.core.database.entity.LinkRequest;
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
    @Inject
    SimpleFloodgateApi api;

    @Inject
    CommonPlayerLink link;

    @Inject
    FloodgateLogger logger;

    @Override
    public Command<PlayerAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager
                .commandBuilder("linkaccount", Description.of("Link your Java account with your Bedrock account"))
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
                sender.sendMessage(CommonCommandMessage.GLOBAL_LINKING_NOTICE, literal("url", Constants.LINK_INFO_URL));
            }
            return;
        } else if (linkState.globalLinkingEnabled()) {
            sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE, literal("url", Constants.LINK_INFO_URL));
        }

        ProfileAudience targetUser = context.get("player");
        // allowUuid is false so username cannot be null
        String targetName = targetUser.username();

        String code = context.getOrDefault("code", null);
        if (code == null) {
            createLinkRequest(sender, targetName);
            return;
        }

        validateCodeAndLink(sender, targetName, code);
    }

    private void createLinkRequest(UserAudience sender, String targetUsername) {
        String code = Utils.generateCode(6);

        String targetPlatform;
        CompletableFuture<Void> future;
        if (api.isBedrockPlayer(sender.uuid())) {
            targetPlatform = "Bedrock";
            future = link.createBedrockLinkRequest(sender.uuid(), sender.username(), targetUsername, code);
        } else {
            targetPlatform = "Java";
            future = link.createJavaLinkRequest(sender.uuid(), sender.username(), targetUsername, code);
        }

        future.whenComplete(($, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(Message.LINK_REQUEST_ERROR);
                return;
            }

            sender.sendMessage(
                    Message.LINK_REQUEST_CREATED,
                    literal("target", targetUsername),
                    literal("target_platform", targetPlatform),
                    literal("command", "/linkaccount " + sender.username() + " " + code),
                    literal("unlink_command", "/unlinkaccount"));
        });
    }

    private void validateCodeAndLink(UserAudience sender, String targetName, String code) {
        boolean isSenderBedrock = api.isBedrockPlayer(sender.uuid());
        CompletableFuture<LinkRequest> linkRequest;
        if (isSenderBedrock) {
            linkRequest = link.linkRequestForBedrock(targetName, sender.username(), code);
        } else {
            linkRequest = link.linkRequestForJava(sender.username(), targetName, code);
            // allow the link requests created in the disconnect screen to work as well
            if (link.allowCreateLinkRequest()) {
                linkRequest = linkRequest.thenCompose(request -> {
                    if (request != null) {
                        return CompletableFuture.completedFuture(request);
                    }
                    return link.linkRequestForJava(targetName, code);
                });
            }
        }

        linkRequest
                .thenApply(request -> {
                    if (request == null) {
                        throw LinkVerificationException.NO_LINK_REQUESTED;
                    }
                    if (request.isExpired(link.verifyLinkTimeout())) {
                        throw LinkVerificationException.LINK_REQUEST_EXPIRED;
                    }
                    // we miss either the Java UUID or the Bedrock UUID depending on which one started the link
                    if (request.bedrockUniqueId() == null) {
                        return request.withBedrockUniqueId(sender.uuid());
                    }
                    // username might be null, depending on whether allowCreateLinkRequest is true
                    return request.withJava(sender.uuid(), sender.username());
                })
                .thenCompose(request -> {
                    return link.addLink(request.javaUniqueId(), request.javaUsername(), request.bedrockUniqueId());
                })
                .whenComplete(($, throwable) -> {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }

                    if (throwable instanceof LinkVerificationException exception) {
                        sender.sendMessage(exception.message(), exception.placeholders());
                        return;
                    }
                    if (throwable != null) {
                        logger.error("Error while executing link command", throwable);
                        sender.sendMessage(Message.LINK_REQUEST_ERROR);
                        return;
                    }

                    if (isSenderBedrock) {
                        sender.disconnect(
                                Message.LINK_REQUEST_COMPLETED,
                                literal("target", targetName),
                                literal("command", "/unlinkaccount"));
                    } else {
                        sender.sendMessage(
                                Message.LINK_REQUEST_COMPLETED,
                                literal("target", targetName),
                                literal("command", "/unlinkaccount"));
                    }
                });
    }

    @Override
    public boolean shouldRegister(FloodgateConfig config) {
        FloodgateConfig.PlayerLinkConfig linkConfig = config.playerLink();
        return linkConfig.enabled() && (linkConfig.enableOwnLinking() || linkConfig.enableGlobalLinking());
    }

    public static final class Message {
        public static final TranslatableMessage ALREADY_LINKED =
                new TranslatableMessage("floodgate.command.link_account.already_linked", MessageType.ERROR);
        public static final TranslatableMessage JAVA_USAGE =
                new TranslatableMessage("floodgate.command.link_account.java_usage", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_CREATED =
                new TranslatableMessage("floodgate.command.link_account.link_request_created", MessageType.SUCCESS);
        public static final TranslatableMessage BEDROCK_USAGE =
                new TranslatableMessage("floodgate.command.link_account.bedrock_usage", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_EXPIRED =
                new TranslatableMessage("floodgate.command.link_account.link_request_expired", MessageType.ERROR);
        public static final TranslatableMessage LINK_REQUEST_COMPLETED =
                new TranslatableMessage("floodgate.command.link_account.link_request_completed", MessageType.SUCCESS);
        public static final TranslatableMessage LINK_REQUEST_ERROR = new TranslatableMessage(
                "floodgate.command.link_account.link_request_error " + CommonCommandMessage.CHECK_CONSOLE,
                MessageType.ERROR);
        public static final TranslatableMessage INVALID_CODE =
                new TranslatableMessage("floodgate.command.link_account.invalid_code", MessageType.ERROR);
        public static final TranslatableMessage NO_LINK_REQUESTED =
                new TranslatableMessage("floodgate.command.link_account.no_link_requested", MessageType.ERROR);
    }
}
