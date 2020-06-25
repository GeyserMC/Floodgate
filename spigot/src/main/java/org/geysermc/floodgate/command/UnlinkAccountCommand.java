package org.geysermc.floodgate.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.AbstractUnlinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class UnlinkAccountCommand extends AbstractUnlinkAccountCommand<Player, CommandUtil> implements CommandExecutor {
    public UnlinkAccountCommand(PlayerLink link, CommandUtil commandUtil) {
        super(link, commandUtil);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CommonMessage.NOT_A_PLAYER.getMessage());
            return true;
        }
        UUID uuid = ((Player) sender).getUniqueId();
        execute((Player) sender, uuid);
        return true;
    }
}
