package org.geysermc.floodgate;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.UUID;

public class UnlinkAccountCommand implements Command {

    @Getter private static PlayerLink playerLink;

    public static void init(PlayerLink passedPlayerLink) {
        playerLink = passedPlayerLink;
    }

    @Override
    public void execute(CommandSource commandSender, String[] args) {
        UUID uuid = ((Player) commandSender).getUniqueId();

        if (PlayerLink.isEnabledAndAllowed()) {
            if (!playerLink.isLinkedPlayer(uuid)) {
                commandSender.sendMessage(TextComponent.of("Your account isn't linked!").color(TextColor.RED));
                return;
            }
            commandSender.sendMessage(playerLink.unlinkPlayer(uuid) ?
                    TextComponent.of("Unlink successful!").color(TextColor.GREEN) :
                    TextComponent.of("An error occurred while unlinking player! Please check the console.").color(TextColor.RED)
            );
        } else {
            commandSender.sendMessage(TextComponent.of("Linking is not enabled on this server.").color(TextColor.RED));
            return;
        }
    }

}
