package org.geysermc.floodgate.util;

import org.geysermc.floodgate.command.CommandMessage;

public interface ICommandUtil<P> {
    String LINK_ACCOUNT_COMMAND = "linkaccount";
    String UNLINK_ACCOUNT_COMMAND = "unlinkaccount";

    /**
     * Send the specified player a message
     *
     * @param player the player to send the message to
     * @param message the command message
     * @param args the arguments
     */
    void sendMessage(P player, CommandMessage message, Object... args);

    /**
     * Same as {@link ICommandUtil#sendMessage(Object, CommandMessage, Object...)} except it kicks the player.
     *
     * @param player the player to send the message to
     * @param message the command message
     * @param args the arguments
     */
    void kickPlayer(P player, CommandMessage message, Object... args);
}
