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

package org.geysermc.floodgate.platform.command;

import org.geysermc.floodgate.platform.command.util.CommandResponseCache;
import org.geysermc.floodgate.util.MessageFormatter;

/**
 * CommandMessage is the interface of a message that can be send to a command source after
 * executing a command. Those messages are generally implemented using enums, so that they are
 * only created once and can be used over and over again. This in combination with
 * {@link CommandResponseCache message caching} should make this system quite fast.
 */
public interface CommandMessage {
    char COLOR_CHAR = '\u00A7';

    /**
     * Returns the message attached to the enum identifier
     */
    String getMessage();

    /**
     * This method will format a message by putting the arguments on the missing spots.
     *
     * @param args the arguments to fill in at the missing spots
     * @return the formatted message
     */
    default String format(Object... args) {
        return MessageFormatter.format(getMessage(), args);
    }
}
