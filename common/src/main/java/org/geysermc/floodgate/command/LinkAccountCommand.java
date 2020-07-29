/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.command;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.LinkRequest;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.link.LinkRequestImpl;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.util.CommandUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@NoArgsConstructor
public final class LinkAccountCommand implements Command {
    private final Map<String, LinkRequest> activeLinkRequests = new HashMap<>();

    @Inject private FloodgateApi api;
    @Inject private CommandUtil commandUtil;

    @Override
    public void execute(Object player, UUID uuid, String username, String[] args) {
        PlayerLink link = api.getPlayerLink();
        if (!link.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINK_REQUEST_DISABLED);
            return;
        }

        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonCommandMessage.IS_LINKED_ERROR);
                return;
            }

            if (linked) {
                sendMessage(player, Message.ALREADY_LINKED);
                return;
            }

            // when the player is a Java player
            if (!api.isBedrockPlayer(uuid)) {
                if (args.length != 1) {
                    sendMessage(player, Message.JAVA_USAGE);
                    return;
                }

                String code = String.format("%04d", new Random().nextInt(10000));
                String bedrockUsername = args[0];

                LinkRequest linkRequest =
                        new LinkRequestImpl(username, uuid, code, bedrockUsername);

                activeLinkRequests.put(username, linkRequest);
                sendMessage(player, Message.LINK_REQUEST_CREATED, bedrockUsername, username, code);
                return;
            }

            // when the player is a Bedrock player

            if (args.length != 2) {
                sendMessage(player, Message.BEDROCK_USAGE);
                return;
            }

            String javaUsername = args[0];
            String code = args[1];
            LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
            if (request == null || !request.isRequestedPlayer(api.getPlayer(uuid))) {
                sendMessage(player, Message.NO_LINK_REQUESTED);
                return;
            }

            if (!request.getLinkCode().equals(code)) {
                sendMessage(player, Message.INVALID_CODE);
                return;
            }

            // Delete the request, whether it has expired or is successful
            activeLinkRequests.remove(javaUsername);
            if (request.isExpired(link.getVerifyLinkTimeout())) {
                sendMessage(player, Message.LINK_REQUEST_EXPIRED);
                return;
            }

            link.linkPlayer(uuid, request.getJavaUniqueId(), request.getJavaUsername())
                    .whenComplete((aVoid, error) -> {
                        if (error != null) {
                            sendMessage(player, Message.LINK_REQUEST_ERROR);
                            return;
                        }
                        commandUtil.kickPlayer(
                                player, Message.LINK_REQUEST_COMPLETED, request.getJavaUsername()
                        );
                    });
        });
    }

    @Override
    public String getName() {
        return "linkaccount";
    }

    @Override
    public String getPermission() {
        return "floodgate.linkaccount";
    }

    @Override
    public boolean isRequirePlayer() {
        return true;
    }

    private void sendMessage(Object player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        ALREADY_LINKED("&cYour account is already linked!\n" +
                "&cIf you want to link to a different account, run &6/unlinkaccount&c and try it again."
        ),
        JAVA_USAGE("&cUsage: /linkaccount <gamertag>"),
        LINK_REQUEST_CREATED("&aLog in as {} on Bedrock and run &6/linkaccount {} {}\n" +
                "&cWarning: Any progress on your Bedrock account will not be carried over! Save any items in your inventory first.\n" +
                "&cIf you change your mind you can run &6/unlinkaccount&c to get your progess back."
        ),
        BEDROCK_USAGE("&cStart the process from Java! Usage: /linkaccount <gamertag>"),
        LINK_REQUEST_EXPIRED("&cThe code you entered is expired! Run &6/linkaccount&c again on your Java account"),
        LINK_REQUEST_COMPLETED("You are successfully linked to {}!\nIf you want to undo this run /unlinkaccount"),
        LINK_REQUEST_ERROR("&cAn error occurred while linking. " + CommonCommandMessage.CHECK_CONSOLE),
        INVALID_CODE("&cInvalid code! Please check your code or run the &6/linkaccount&c command again on your Java account."),
        NO_LINK_REQUESTED("&cThis player has not requested an account link! Please log in on Java and request one with &6/linkaccount"),
        LINK_REQUEST_DISABLED("&cLinking is not enabled on this server.");

        @Getter private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
