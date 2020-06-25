package org.geysermc.floodgate.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.AbstractLinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class LinkAccountCommand extends AbstractLinkAccountCommand<Player, CommandUtil> implements CommandExecutor {
    public LinkAccountCommand(PlayerLink link, CommandUtil commandUtil) {
        super(link, commandUtil);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CommonMessage.NOT_A_PLAYER.getMessage());
            return true;
        }
        UUID uuid = ((Player) sender).getUniqueId();
        String username = sender.getName();
        execute((Player) sender, uuid, username, args);
        return true;
    }
}
