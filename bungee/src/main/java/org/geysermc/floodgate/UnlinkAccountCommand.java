package org.geysermc.floodgate;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class UnlinkAccountCommand extends Command {

    public UnlinkAccountCommand() {
        super("unlinkaccount");
    }
    @Getter private static PlayerLink playerLink;

    public static void init(PlayerLink passedPlayerLink) {
        playerLink = passedPlayerLink;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        UUID uuid = ((ProxiedPlayer) commandSender).getUniqueId();

        if (PlayerLink.isEnabledAndAllowed()) {
            if (!playerLink.isLinkedPlayer(uuid)) {
                commandSender.sendMessage(new ComponentBuilder("Your account isn't linked!").color(ChatColor.RED).create());
                return;
            }
            commandSender.sendMessage(playerLink.unlinkPlayer(uuid) ?
                    new ComponentBuilder("Unlink successful!").color(ChatColor.GREEN).create() :
                    new ComponentBuilder("An error occurred while unlinking player! Please check the console.").color(ChatColor.RED).create()
            );
        } else {
            commandSender.sendMessage(new ComponentBuilder("Linking is not enabled on this server.").color(ChatColor.RED).create());
            return;
        }
    }

}
