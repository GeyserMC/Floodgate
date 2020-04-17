package org.geysermc.floodgate;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class LinkAccountCommand extends Command {

    static final Map<String, LinkRequest> activeLinkRequests = new HashMap<>(); // Maps Java usernames to LinkRequest objects

    public LinkAccountCommand() {
        super("linkaccount");
    }
    @Getter private static PlayerLink playerLink;

    public static void init(PlayerLink passedPlayerLink) {
        playerLink = passedPlayerLink;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        UUID uuid = ((ProxiedPlayer) commandSender).getUniqueId();
        String username = commandSender.getName();

        if (PlayerLink.isEnabledAndAllowed()) {
            if (playerLink.isLinkedPlayer(uuid)) {
                commandSender.sendMessage(new ComponentBuilder("Your account is already linked!").color(ChatColor.RED).create());
                commandSender.sendMessage(new ComponentBuilder("If you want to link to a different account, run ").color(ChatColor.RED)
                        .append("/unlinkaccount").color(ChatColor.GOLD)
                        .append(" and try it again.").color(ChatColor.RED).create());
                return;
            }
            // when the player is a Java player
            if (!AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
                if (args.length != 1) {
                    commandSender.sendMessage(new ComponentBuilder("Usage: /linkaccount <gamertag>").color(ChatColor.RED).create());
                    return;
                }
                String code = String.format("%04d", new Random().nextInt(10000));
                String bedrockUsername = args[0];
                activeLinkRequests.put(username, new LinkRequest(username, uuid, code, bedrockUsername));
                commandSender.sendMessage(new ComponentBuilder("Log in as " + bedrockUsername + " on Bedrock and run ").color(ChatColor.GREEN)
                        .append("/linkaccount " + username + " " + code).color(ChatColor.GOLD).create());
                commandSender.sendMessage(new ComponentBuilder("Warning: Any progress on your Bedrock account will not be carried over! Save any items in your inventory first.").color(ChatColor.RED).create());
                commandSender.sendMessage(new ComponentBuilder("If you change your mind you can run ").color(ChatColor.RED)
                        .append("/unlinkaccount").color(ChatColor.GOLD)
                        .append(" to get your progess back.").color(ChatColor.RED).create());
                return;
            }
            // when the player is a Bedrock player
            if (args.length != 2) {
                commandSender.sendMessage(new ComponentBuilder("Start the process from Java! Usage: /linkaccount <gamertag>").color(ChatColor.RED).create());
                return;
            }
            String javaUsername = args[0];
            String code = args[1];
            LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
            if (request != null && request.checkGamerTag(AbstractFloodgateAPI.getPlayer(uuid))) {
                if (request.linkCode.equals(code)) {
                    activeLinkRequests.remove(javaUsername); // Delete the request, whether it has expired or is successful
                    if (request.isExpired()) {
                        commandSender.sendMessage(new ComponentBuilder("The code you entered is expired! Run ").color(ChatColor.RED)
                                .append("/linkaccount").color(ChatColor.GOLD)
                                .append(" again on your Java account.").color(ChatColor.RED).create());
                        return;
                    }
                    if (playerLink.linkPlayer(uuid, request.javaUniqueId, request.javaUsername)) {
                        ((ProxiedPlayer) commandSender).disconnect(new ComponentBuilder("You are successfully linked to " + request.javaUsername + "!\nIf you want to undo this run /unlinkaccount").create());
                        return;
                    }
                    commandSender.sendMessage(new ComponentBuilder("An error occurred while linking. Please check the console.").color(ChatColor.RED).create());
                    return;
                }
                commandSender.sendMessage(new ComponentBuilder("Invalid code! Please check your code or run the ").color(ChatColor.RED)
                        .append("/unlinkaccount").color(ChatColor.GOLD)
                        .append(" command again on your Java account.").color(ChatColor.RED).create());
                return;
            }
            commandSender.sendMessage(new ComponentBuilder("This player has not requested an account link! Please log in on Java and request one with ").color(ChatColor.RED)
                            .append("/linkaccount").color(ChatColor.GOLD).create());
            return;
        }
        commandSender.sendMessage(new ComponentBuilder("Linking is not enabled on this server.").color(ChatColor.RED).create());
        return;
    }

}
