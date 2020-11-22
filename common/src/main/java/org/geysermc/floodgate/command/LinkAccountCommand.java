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

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.LinkRequest;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.link.LinkRequestImpl;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;

@NoArgsConstructor
public final class LinkAccountCommand implements Command {
    private final Map<String, LinkRequest> activeLinkRequests = new HashMap<>();

    @Inject private FloodgateApi api;
    @Inject private CommandUtil commandUtil;

//    @Override todo impl this
//    public <T> LiteralCommandNode<T> commandNode(T source, CommandUtil commandUtil) {
//        return literal(getName())
//                .then(
//                        argument("gamertag", word())
//                                .executes(cmd -> {
//                                    return 0;
//                                })
//                ).build();
//    }

    @Override
    public void execute(Object player, UUID uuid, String username, String locale, String[] args) {
        PlayerLink link = api.getPlayerLink();
        if (!link.isEnabledAndAllowed()) {
            sendMessage(player, locale, Message.LINK_REQUEST_DISABLED);
            return;
        }

        link.isLinkedPlayer(uuid)
                .whenComplete((linked, throwable) -> {
                    if (throwable != null) {
                        sendMessage(player, locale, CommonCommandMessage.IS_LINKED_ERROR);
                        return;
                    }

                    if (linked) {
                        sendMessage(player, locale, Message.ALREADY_LINKED);
                        return;
                    }

                    // when the player is a Java player
                    if (!api.isBedrockPlayer(uuid)) {
                        if (args.length != 1) {
                            sendMessage(player, locale, Message.JAVA_USAGE);
                            return;
                        }

                        String code = String.format("%04d", new Random().nextInt(10000));
                        String bedrockUsername = args[0];

                        LinkRequest linkRequest =
                                new LinkRequestImpl(username, uuid, code, bedrockUsername);

                        activeLinkRequests.put(username, linkRequest);
                        sendMessage(
                                player, locale, Message.LINK_REQUEST_CREATED,
                                bedrockUsername, username, code
                        );
                        return;
                    }

                    // when the player is a Bedrock player

                    if (args.length != 2) {
                        sendMessage(player, locale, Message.BEDROCK_USAGE);
                        return;
                    }

                    String javaUsername = args[0];
                    String code = args[1];
                    LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
                    if (request == null || !request.isRequestedPlayer(api.getPlayer(uuid))) {
                        sendMessage(player, locale, Message.NO_LINK_REQUESTED);
                        return;
                    }

                    if (!request.getLinkCode().equals(code)) {
                        sendMessage(player, locale, Message.INVALID_CODE);
                        return;
                    }

                    // Delete the request, whether it has expired or is successful
                    activeLinkRequests.remove(javaUsername);
                    if (request.isExpired(link.getVerifyLinkTimeout())) {
                        sendMessage(player, locale, Message.LINK_REQUEST_EXPIRED);
                        return;
                    }

                    link.linkPlayer(uuid, request.getJavaUniqueId(), request.getJavaUsername())
                            .whenComplete((unused, error) -> {
                                if (error != null) {
                                    sendMessage(player, locale, Message.LINK_REQUEST_ERROR);
                                    return;
                                }
                                commandUtil.kickPlayer(
                                        player, locale, Message.LINK_REQUEST_COMPLETED,
                                        request.getJavaUsername()
                                );
                            });
                });
    }

    @Override
    public String getName() {
        return "linkaccount";
    }

    @Override
    public String getDescription() {
        return "Link your Java account with your Bedrock account";
    }

    @Override
    public String getPermission() {
        return "floodgate.linkaccount";
    }

    @Override
    public boolean isRequirePlayer() {
        return true;
    }

    private void sendMessage(Object player, String locale, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, locale, message, args);
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
