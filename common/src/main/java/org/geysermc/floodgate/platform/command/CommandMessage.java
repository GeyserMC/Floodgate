/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.platform.command;

import org.geysermc.floodgate.util.LanguageManager;

/**
 * CommandMessage is the interface of a message that can be send to a command source after executing
 * a command. Messages are generally implemented using enums.
 */
public interface CommandMessage {
    /**
     * Returns the message attached to the enum identifier
     */
    String getRawMessage();

    /**
     * Returns the parts of this message (getRawMessage() split on " ")
     */
    String[] getTranslateParts();

    default String translateMessage(LanguageManager manager, String locale, Object... args) {
        String[] translateParts = getTranslateParts();
        if (translateParts.length == 1) {
            return manager.getString(getRawMessage(), locale, args);
        }
        // todo only works when one section has arguments
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < translateParts.length; i++) {
            builder.append(manager.getString(translateParts[i], locale, args));
            if (++i != translateParts.length) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
}
