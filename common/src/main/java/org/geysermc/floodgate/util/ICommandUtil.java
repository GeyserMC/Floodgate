package org.geysermc.floodgate.util;

import org.geysermc.floodgate.command.CommandMessage;

public interface ICommandUtil<P> {
    String LINK_ACCOUNT_COMMAND = "linkaccount";
    String UNLINK_ACCOUNT_COMMAND = "unlinkaccount";

    /**
     * Send the specified player a message
     */
    void sendMessage(P player, CommandMessage message, Object... args);

    /**
     * Same as {@link #sendMessage(P, CommandMessage, Object...)} except it kicks the player.
     */
    void kickPlayer(P player, CommandMessage message, Object... args);
}
