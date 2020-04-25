package org.geysermc.floodgate.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.geysermc.floodgate.AbstractLinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class LinkAccountCommand extends AbstractLinkAccountCommand<Player, CommandUtil> implements Command {
    public LinkAccountCommand(PlayerLink link, CommandUtil commandUtil) {
        super(link, commandUtil);
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getCommandUtil().getOrAddCachedMessage(CommonMessage.NOT_A_PLAYER));
            return;
        }
        UUID uuid = ((Player) sender).getUniqueId();
        String username = ((Player) sender).getUsername();
        execute((Player) sender, uuid, username, args);
    }
}
