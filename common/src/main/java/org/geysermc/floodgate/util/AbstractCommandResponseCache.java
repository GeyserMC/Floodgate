package org.geysermc.floodgate.util;

import org.geysermc.floodgate.command.CommandMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @param <TYPE> Message Type stored. For example BaseComponent[] for Bungeecord
 */
public abstract class AbstractCommandResponseCache<TYPE> {
    private final Map<CommandMessage, TYPE> cachedResponses = new HashMap<>(0);

    /**
     * Transforms a string (raw input) into a format that can be sent to the player
     */
    protected abstract TYPE transformMessage(String message);

    /**
     * If the message has no arguments:<br>
     *     If cached: return cached message.<br>
     *     If not cached: transform it, add the message to cache and return the message.<br>
     * It will only transform the message if the message has one or more arguments
     */
    public TYPE getOrAddCachedMessage(CommandMessage message, Object... args) {
        if (args != null && args.length > 0) {
            return transformMessage(format(message, args));
        }
        if (!cachedResponses.containsKey(message)) {
            TYPE components = transformMessage(message.getMessage());
            cachedResponses.put(message, components);
            return components;
        }
        return cachedResponses.get(message);
    }

    protected String format(CommandMessage message, Object... args) {
        String msg = message.getMessage();
        return args != null && args.length > 0 ? String.format(msg, args) : msg;
    }
}
