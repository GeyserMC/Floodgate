package org.geysermc.floodgate;

import lombok.Getter;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class LinkAccountCommand implements Command {

    static final Map<String, LinkRequest> activeLinkRequests = new HashMap<>(); // Maps Java usernames to LinkRequest objects

    @Getter private static PlayerLink playerLink;

    public static void init(PlayerLink passedPlayerLink) {
        playerLink = passedPlayerLink;
    }

    @Override
    public void execute(CommandSource commandSender, String[] args) {
        UUID uuid = ((Player) commandSender).getUniqueId();
        String username = ((Player) commandSender).getUsername();

        if (PlayerLink.isEnabledAndAllowed()) {
            if (playerLink.isLinkedPlayer(uuid)) {
                commandSender.sendMessage(TextComponent.of("Your account is already linked!").color(TextColor.RED));
                commandSender.sendMessage(TextComponent.of("If you want to link to a different account, run ").color(TextColor.RED)
                        .append(TextComponent.of("/unlinkaccount").color(TextColor.GOLD))
                        .append(TextComponent.of(" and try it again.").color(TextColor.RED)));
                return;
            }
            // when the player is a Java player
            if (!AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
                if (args.length != 1) {
                    commandSender.sendMessage(TextComponent.of("Usage: /linkaccount <gamertag>").color(TextColor.RED));
                    return;
                }
                String code = String.format("%04d", new Random().nextInt(10000));
                String bedrockUsername = args[0];
                activeLinkRequests.put(username, new LinkRequest(username, uuid, code, bedrockUsername));
                commandSender.sendMessage(TextComponent.of("Log in as " + bedrockUsername + " on Bedrock and run ").color(TextColor.GREEN)
                        .append(TextComponent.of("/linkaccount " + username + " " + code).color(TextColor.GOLD)));
                commandSender.sendMessage(TextComponent.of("Warning: Any progress on your Bedrock account will not be carried over! Save any items in your inventory first.").color(TextColor.RED));
                commandSender.sendMessage(TextComponent.of("If you change your mind you can run ").color(TextColor.RED)
                        .append(TextComponent.of("/unlinkaccount").color(TextColor.GOLD))
                        .append(TextComponent.of(" to get your progess back.").color(TextColor.RED)));
                return;
            }
            // when the player is a Bedrock player
            if (args.length != 2) {
                commandSender.sendMessage(TextComponent.of("Start the process from Java! Usage: /linkaccount <gamertag>").color(TextColor.RED));
                return;
            }
            String javaUsername = args[0];
            String code = args[1];
            LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
            if (request != null && request.checkGamerTag(AbstractFloodgateAPI.getPlayer(uuid))) {
                if (request.linkCode.equals(code)) {
                    activeLinkRequests.remove(javaUsername); // Delete the request, whether it has expired or is successful
                    if (request.isExpired()) {
                        commandSender.sendMessage(TextComponent.of("The code you entered is expired! Run ").color(TextColor.RED)
                                .append(TextComponent.of("/linkaccount").color(TextColor.GOLD))
                                .append(TextComponent.of(" again on your Java account.").color(TextColor.RED)));
                        return;
                    }
                    if (playerLink.linkPlayer(uuid, request.javaUniqueId, request.javaUsername)) {
                        ((Player) commandSender).disconnect(TextComponent.of("You are successfully linked to " + request.javaUsername + "!\nIf you want to undo this run /unlinkaccount"));
                        return;
                    }
                    commandSender.sendMessage(TextComponent.of("An error occurred while linking. Please check the console.").color(TextColor.RED));
                    return;
                }
                commandSender.sendMessage(TextComponent.of("Invalid code! Please check your code or run the ").color(TextColor.RED)
                        .append(TextComponent.of("/unlinkaccount").color(TextColor.GOLD))
                        .append(TextComponent.of(" command again on your Java account.").color(TextColor.RED)));
                return;
            }
            commandSender.sendMessage(TextComponent.of("This player has not requested an account link! Please log in on Java and request one with ").color(TextColor.RED)
                    .append(TextComponent.of("/linkaccount")).color(TextColor.GOLD));
            return;
        }
        commandSender.sendMessage(TextComponent.of("Linking is not enabled on this server.").color(TextColor.RED));
        return;
    }

}
