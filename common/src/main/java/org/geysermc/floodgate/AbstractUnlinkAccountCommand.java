package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;
import org.geysermc.floodgate.util.CommonMessage;
import org.geysermc.floodgate.util.ICommandUtil;

import java.util.UUID;

@AllArgsConstructor
public class AbstractUnlinkAccountCommand<PLAYER, COMMAND_UTIL extends ICommandUtil<PLAYER>> {
    private final PlayerLink link;
    @Getter(AccessLevel.PROTECTED)
    private final COMMAND_UTIL commandUtil;

    public void execute(PLAYER player, UUID uuid) {
        if (!PlayerLink.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINKING_NOT_ENABLED);
            return;
        }
        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonMessage.IS_LINKED_ERROR);
                return;
            }
            if (!linked) {
                sendMessage(player, Message.NOT_LINKED);
                return;
            }
            link.unlinkPlayer(uuid).whenComplete((aVoid, throwable1) ->
                    sendMessage(player, throwable1 == null ? Message.UNLINK_SUCCESS : Message.UNLINK_ERROR)
            );
        });
    }

    private void sendMessage(PLAYER player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        NOT_LINKED("&cYour account isn't linked"),
        UNLINK_SUCCESS("&cUnlink successful! Rejoin to return to your Bedrock account"),
        UNLINK_ERROR("&cAn error occurred while unlinking player! " + CommonMessage.CHECK_CONSOLE),
        LINKING_NOT_ENABLED("&cLinking is not enabled on this server");

        @Getter private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
