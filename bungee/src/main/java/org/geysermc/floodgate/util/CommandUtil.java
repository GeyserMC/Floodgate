package org.geysermc.floodgate.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.floodgate.command.CommandMessage;

public class CommandUtil extends AbstractCommandResponseCache<BaseComponent[]> implements ICommandUtil<ProxiedPlayer> {
    @Override
    public void sendMessage(ProxiedPlayer player, CommandMessage message, Object... args) {
        player.sendMessage(getOrAddCachedMessage(message));
    }

    @Override
    public void kickPlayer(ProxiedPlayer player, CommandMessage message, Object... args) {
        player.disconnect(getOrAddCachedMessage(message));
    }

    @Override
    protected BaseComponent[] transformMessage(String message) {
        return TextComponent.fromLegacyText(message);
    }
}
