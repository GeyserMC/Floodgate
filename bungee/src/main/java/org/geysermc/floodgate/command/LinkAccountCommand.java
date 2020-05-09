package org.geysermc.floodgate.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.geysermc.floodgate.AbstractLinkAccountCommand;
import org.geysermc.floodgate.PlayerLink;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.CommonMessage;

import java.util.UUID;

public class LinkAccountCommand extends Command {
    private final LinkAccountCommandHandler handler;

    public LinkAccountCommand(PlayerLink playerLink, CommandUtil responseCache) {
        super(CommandUtil.LINK_ACCOUNT_COMMAND);
        handler = new LinkAccountCommandHandler(playerLink, responseCache);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(CommonMessage.NOT_A_PLAYER.getMessage());
            return;
        }
        UUID uuid = ((ProxiedPlayer) sender).getUniqueId();
        String username = sender.getName();
        handler.execute((ProxiedPlayer) sender, uuid, username, args);
    }

    private static class LinkAccountCommandHandler extends AbstractLinkAccountCommand<ProxiedPlayer, CommandUtil> {
        public LinkAccountCommandHandler(PlayerLink link, CommandUtil commandUtil) {
            super(link, commandUtil);
        }
    }
}
