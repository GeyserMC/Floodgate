package org.geysermc.floodgate.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.geysermc.floodgate.AbstractUnlinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class UnlinkAccountCommand extends AbstractUnlinkAccountCommand<Player, CommandUtil> implements Command {
    public UnlinkAccountCommand(PlayerLink link, CommandUtil commandUtil) {
        super(link, commandUtil);
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getCommandUtil().getOrAddCachedMessage(CommonMessage.NOT_A_PLAYER));
            return;
        }
        UUID uuid = ((Player) sender).getUniqueId();
        execute((Player) sender, uuid);
    }
}
