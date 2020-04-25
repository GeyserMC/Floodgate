package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;
import org.geysermc.floodgate.link.SQLiteImpl;
import org.geysermc.floodgate.util.ICommandUtil;

import java.util.UUID;

@AllArgsConstructor
public class AbstractUnlinkAccountCommand<PLAYER, COMMAND_UTIL extends ICommandUtil<PLAYER>> {
    private final PlayerLink link;
    @Getter(AccessLevel.PROTECTED)
    private final COMMAND_UTIL commandUtil;

    public void execute(PLAYER player, UUID uuid) {
        if (!SQLiteImpl.isEnabledAndAllowed()) sendMessage(player, Message.LINKING_NOT_ENABLED);
        if (!link.isLinkedPlayer(uuid)) {
            sendMessage(player, Message.NOT_LINKED);
            return;
        }
        sendMessage(player, link.unlinkPlayer(uuid) ? Message.UNLINK_SUCCESS : Message.UNLINK_ERROR);
    }

    private void sendMessage(PLAYER player, Message message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        NOT_LINKED("&cYour account isn't linked"),
        UNLINK_SUCCESS("&cUnlink successful!"),
        UNLINK_ERROR("&cAn error occurred while unlinking player! Please check the console"),
        LINKING_NOT_ENABLED("&cLinking is not enabled on this server");

        @Getter private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
