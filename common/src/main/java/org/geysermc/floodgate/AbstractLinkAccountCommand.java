package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;
import org.geysermc.floodgate.util.CommonMessage;
import org.geysermc.floodgate.util.ICommandUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@AllArgsConstructor
public class AbstractLinkAccountCommand<PLAYER, COMMAND_UTIL extends ICommandUtil<PLAYER>> {
    private final Map<String, LinkRequest> activeLinkRequests = new HashMap<>();
    private final PlayerLink link;
    @Getter(AccessLevel.PROTECTED)
    private final COMMAND_UTIL commandUtil;

    public void execute(PLAYER player, UUID uuid, String username, String[] args) {
        if (!PlayerLink.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINK_REQUEST_DISABLED);
            return;
        }
        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonMessage.IS_LINKED_ERROR);
                return;
            }
            if (linked) {
                sendMessage(player, Message.ALREADY_LINKED);
                return;
            }
            // when the player is a Java player
            if (!AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
                if (args.length != 1) {
                    sendMessage(player, Message.JAVA_USAGE);
                    return;
                }
                String code = String.format("%04d", new Random().nextInt(10000));
                String bedrockUsername = args[0];
                activeLinkRequests.put(username, new LinkRequest(username, uuid, code, bedrockUsername));
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
            if (request != null && request.checkGamerTag(AbstractFloodgateAPI.getPlayer(uuid))) {
                if (request.getLinkCode().equals(code)) {
                    activeLinkRequests.remove(javaUsername); // Delete the request, whether it has expired or is successful
                    if (request.isExpired()) {
                        sendMessage(player, Message.LINK_REQUEST_EXPIRED);
                        return;
                    }
                    link.linkPlayer(uuid, request.getJavaUniqueId(), request.getJavaUsername()).whenComplete((aVoid, throwable1) -> {
                        if (throwable1 != null) {
                            sendMessage(player, Message.LINK_REQUEST_ERROR);
                            return;
                        }
                        commandUtil.kickPlayer(player, Message.LINK_REQUEST_COMPLETED, request.getJavaUsername());
                    });
                    return;
                }
                sendMessage(player, Message.INVALID_CODE);
                return;
            }
            sendMessage(player, Message.NO_LINK_REQUESTED);
        });
    }

    private void sendMessage(PLAYER player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        ALREADY_LINKED(
                "&cYour account is already linked!\n" +
                "&cIf you want to link to a different account, run &6/unlinkaccount&c and try it again."
        ),
        JAVA_USAGE("&cUsage: /linkaccount <gamertag>"),
        LINK_REQUEST_CREATED(
                "&aLog in as %s on Bedrock and run &6/linkaccount %s %s\n" +
                "&cWarning: Any progress on your Bedrock account will not be carried over! Save any items in your inventory first.\n" +
                "&cIf you change your mind you can run &6/unlinkaccount&c to get your progess back."
        ),
        BEDROCK_USAGE("&cStart the process from Java! Usage: /linkaccount <gamertag>"),
        LINK_REQUEST_EXPIRED("&cThe code you entered is expired! Run &6/linkaccount&c again on your Java account"),
        LINK_REQUEST_COMPLETED("You are successfully linked to %s!\nIf you want to undo this run /unlinkaccount"),
        LINK_REQUEST_ERROR("&cAn error occurred while linking. " + CommonMessage.CHECK_CONSOLE),
        INVALID_CODE("&cInvalid code! Please check your code or run the &6/linkaccount&c command again on your Java account."),
        NO_LINK_REQUESTED("&cThis player has not requested an account link! Please log in on Java and request one with &6/linkaccount"),
        LINK_REQUEST_DISABLED("&cLinking is not enabled on this server.");

        @Getter private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
