package org.geysermc.floodgate.util;

import org.geysermc.floodgate.command.CommandMessage;

public interface ICommandUtil<PLAYER> {
    String LINK_ACCOUNT_COMMAND = "linkaccount";
    String UNLINK_ACCOUNT_COMMAND = "unlinkaccount";

    /**
     * Send the specified player a message
     */
    void sendMessage(PLAYER player, CommandMessage message, Object... args);

    /**
     * Same as {@link #sendMessage(PLAYER, CommandMessage, Object...)} except it kicks the player.
     */
    void kickPlayer(PLAYER player, CommandMessage message, Object... args);
}
