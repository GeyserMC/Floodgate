package org.geysermc.floodgate.util;

import com.velocitypowered.api.proxy.Player;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.floodgate.command.CommandMessage;

public class CommandUtil extends AbstractCommandResponseCache<TextComponent> implements ICommandUtil<Player> {
    @Override
    public void sendMessage(Player player, CommandMessage message, Object... args) {
        player.sendMessage(getOrAddCachedMessage(message, args));
    }

    @Override
    public void kickPlayer(Player player, CommandMessage message, Object... args) {
        player.disconnect(getOrAddCachedMessage(message, args));
    }

    @Override
    protected TextComponent transformMessage(String message) {
        return LegacyComponentSerializer.legacy().deserialize(message);
    }
}
