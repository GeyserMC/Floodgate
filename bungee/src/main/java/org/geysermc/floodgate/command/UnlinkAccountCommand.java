package org.geysermc.floodgate.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.geysermc.floodgate.AbstractUnlinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class UnlinkAccountCommand extends Command {
    private final UnlinkAccountCommandHandler handler;

    public UnlinkAccountCommand(PlayerLink playerLink, CommandUtil commandUtil) {
        super(CommandUtil.UNLINK_ACCOUNT_COMMAND);
        handler = new UnlinkAccountCommandHandler(playerLink, commandUtil);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(CommonMessage.NOT_A_PLAYER.getMessage());
            return;
        }
        UUID uuid = ((ProxiedPlayer) sender).getUniqueId();
        handler.execute((ProxiedPlayer) sender, uuid);
    }

    private static class UnlinkAccountCommandHandler extends AbstractUnlinkAccountCommand<ProxiedPlayer, CommandUtil> {
        public UnlinkAccountCommandHandler(PlayerLink link, CommandUtil commandUtil) {
            super(link, commandUtil);
        }
    }
}
