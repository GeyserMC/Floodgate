/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.Description;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.LinkRequest;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.link.LinkRequestImpl;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudience.PlayerAudience;
import org.geysermc.floodgate.player.UserAudienceArgument;

@NoArgsConstructor
public final class LinkAccountCommand implements FloodgateCommand {
    private final Map<String, LinkRequest> activeLinkRequests = new HashMap<>();

    @Inject private FloodgateApi api;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager.commandBuilder("linkaccount",
                Description.of("Link your Java account with your Bedrock account"))
                .senderType(PlayerAudience.class)
                .permission("floodgate.command.linkaccount")
                .argument(UserAudienceArgument.of("player", true))
                .argument(StringArgument.optional("code"))
                .handler(this::execute)
                .build();
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.getSender();

        PlayerLink link = api.getPlayerLink();
        if (!link.isEnabledAndAllowed()) {
            sender.sendMessage(Message.LINK_REQUEST_DISABLED);
            return;
        }

        link.isLinkedPlayer(sender.uuid())
                .whenComplete((linked, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(CommonCommandMessage.IS_LINKED_ERROR);
                        return;
                    }

                    if (linked) {
                        sender.sendMessage(Message.ALREADY_LINKED);
                        return;
                    }

                    // when the player is a Java player
                    if (!api.isBedrockPlayer(sender.uuid())) {
                        if (context.contains("code")) {
                            sender.sendMessage(Message.JAVA_USAGE);
                            return;
                        }

                        String code = String.format("%04d", new Random().nextInt(10000));
                        UserAudience targetUser = context.get("player");
                        String targetName = targetUser.username();

                        LinkRequest linkRequest = new LinkRequestImpl(
                                sender.username(), sender.uuid(), code, targetName);

                        activeLinkRequests.put(sender.username(), linkRequest);
                        sender.sendMessage(
                                Message.LINK_REQUEST_CREATED,
                                targetName, sender.username(), code);
                        return;
                    }

                    // when the player is a Bedrock player

                    if (!context.contains("code")) {
                        sender.sendMessage(Message.BEDROCK_USAGE);
                        return;
                    }

                    UserAudience targetUser = context.get("player");
                    String targetName = targetUser.username();
                    String code = context.get("code");

                    LinkRequest request = activeLinkRequests.getOrDefault(targetName, null);
                    if (request == null ||
                            !request.isRequestedPlayer(api.getPlayer(sender.uuid()))) {
                        sender.sendMessage(Message.NO_LINK_REQUESTED);
                        return;
                    }

                    if (!request.getLinkCode().equals(code)) {
                        sender.sendMessage(Message.INVALID_CODE);
                        return;
                    }

                    // Delete the request, whether it has expired or is successful
                    activeLinkRequests.remove(targetName);
                    if (request.isExpired(link.getVerifyLinkTimeout())) {
                        sender.sendMessage(Message.LINK_REQUEST_EXPIRED);
                        return;
                    }

                    link.linkPlayer(sender.uuid(), request.getJavaUniqueId(),
                            request.getJavaUsername())
                            .whenComplete((unused, error) -> {
                                if (error != null) {
                                    sender.sendMessage(Message.LINK_REQUEST_ERROR);
                                    return;
                                }
                                sender.disconnect(
                                        Message.LINK_REQUEST_COMPLETED,
                                        request.getJavaUsername()
                                );
                            });
                });
    }

    @Override
    public boolean shouldRegister(FloodgateConfig config) {
        return !config.getPlayerLink().isUseGlobalLinking();
    }

    @Getter
    public enum Message implements CommandMessage {
        ALREADY_LINKED("floodgate.command.link_account.already_linked"),
        JAVA_USAGE("floodgate.command.link_account.java_usage"),
        LINK_REQUEST_CREATED("floodgate.command.link_account.link_request_created"),
        BEDROCK_USAGE("floodgate.command.link_account.bedrock_usage"),
        LINK_REQUEST_EXPIRED("floodgate.command.link_account.link_request_expired"),
        LINK_REQUEST_COMPLETED("floodgate.command.link_account.link_request_completed"),
        LINK_REQUEST_ERROR("floodgate.command.link_request.error " + CHECK_CONSOLE),
        INVALID_CODE("floodgate.command.link_account.invalid_code"),
        NO_LINK_REQUESTED("floodgate.command.link_account.no_link_requested"),
        LINK_REQUEST_DISABLED("floodgate.commands.linking_disabled");

        private final String rawMessage;
        private final String[] translateParts;

        Message(String rawMessage) {
            this.rawMessage = rawMessage;
            this.translateParts = rawMessage.split(" ");
        }
    }
}
