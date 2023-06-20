/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.command;

import lombok.Getter;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;

/**
 * Messages (or part of messages) that are used in two or more commands and thus are 'commonly
 * used'
 */
@Getter
public enum CommonCommandMessage implements TranslatableMessage {
    LINKING_DISABLED("floodgate.commands.linking_disabled"),
    NOT_A_PLAYER("floodgate.commands.not_a_player"),
    CHECK_CONSOLE("floodgate.commands.check_console"),
    IS_LINKED_ERROR("floodgate.commands.is_linked_error"),
    LOCAL_LINKING_NOTICE("floodgate.commands.local_linking_notice"),
    GLOBAL_LINKING_NOTICE("floodgate.commands.global_linking_notice");

    private final String rawMessage;
    private final String[] translateParts;

    CommonCommandMessage(String rawMessage) {
        this.rawMessage = rawMessage;
        this.translateParts = rawMessage.split(" ");
    }

    @Override
    public String toString() {
        return getRawMessage();
    }
}
