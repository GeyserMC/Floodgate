package org.geysermc.floodgate.util;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.command.CommandMessage;

public class CommandUtil extends AbstractCommandResponseCache<String> implements ICommandUtil<Player> {
    @Override
    public void sendMessage(Player player, CommandMessage message, Object... args) {
        player.sendMessage(format(message, args));
    }

    @Override
    public void kickPlayer(Player player, CommandMessage message, Object... args) {
        player.kickPlayer(format(message, args));
    }

    @Override
    protected String transformMessage(String message) {
        // unlike others, Bukkit doesn't have to transform a message into another class.
        return message;
    }
}
