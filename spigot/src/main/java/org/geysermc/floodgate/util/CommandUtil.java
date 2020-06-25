package org.geysermc.floodgate.util;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.command.CommandMessage;

@AllArgsConstructor
public class CommandUtil extends AbstractCommandResponseCache<String> implements ICommandUtil<Player> {
    private final Plugin plugin;

    @Override
    public void sendMessage(Player player, CommandMessage message, Object... args) {
        player.sendMessage(format(message, args));
    }

    @Override
    public void kickPlayer(Player player, CommandMessage message, Object... args) {
        // Have to run this in a non async thread so we don't get a `Asynchronous player kick!` error
        Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(format(message, args)));
    }

    @Override
    protected String transformMessage(String message) {
        // unlike others, Bukkit doesn't have to transform a message into another class.
        return message;
    }
}
