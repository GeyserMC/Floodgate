/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.platform.command.util;

import org.geysermc.floodgate.platform.command.CommandMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * A class used in all platforms to cache simple messages (messages without arguments) to improve
 * execution speed.
 *
 * @param <T> the message type (of the platform) that is send to the player.
 *           For example BaseComponent[] on Bungeecord. Please note that not every platform has
 *           something like that (for example Bukkit) and then a String should be used instead.
 */
public abstract class CommandResponseCache<T> {
    private final Map<CommandMessage, T> cachedResponses = new HashMap<>(0);

    /**
     * Transforms a string (raw input) into a format that can be send to the player.
     *
     * @param message the message to transform
     * @return the transformed message
     */
    protected abstract T transformMessage(String message);

    /**
     * Get the cached message or (if it isn't cached) {@link #transformMessage(String) transform
     * the message} and add it to the cached messages.
     * Please note that the transformed message will only be added to the cached messages when the
     * transformed message has zero arguments, and thus can be cached.
     *
     * @param message the command message
     * @param args    the arguments
     * @return the transformed (and maybe cached) message.
     */
    public T getOrAddCachedMessage(CommandMessage message, Object... args) {
        if (args != null && args.length > 0) {
            return transformMessage(format(message, args));
        }
        if (!cachedResponses.containsKey(message)) {
            T components = transformMessage(message.getMessage());
            cachedResponses.put(message, components);
            return components;
        }
        return cachedResponses.get(message);
    }

    protected String format(CommandMessage message, Object... args) {
        return args != null && args.length > 0 ?
                message.format(args) :
                message.getMessage();
    }
}