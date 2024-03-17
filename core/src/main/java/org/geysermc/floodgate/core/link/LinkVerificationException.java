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

package org.geysermc.floodgate.core.link;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import org.geysermc.floodgate.core.command.LinkAccountCommand.Message;
import org.geysermc.floodgate.core.platform.command.Placeholder;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;

public class LinkVerificationException extends RuntimeException {
    public static final LinkVerificationException NO_LINK_REQUESTED =
            new LinkVerificationException(Message.NO_LINK_REQUESTED, literal("command", "/linkaccount"));
    public static final LinkVerificationException INVALID_CODE =
            new LinkVerificationException(Message.INVALID_CODE, literal("command", "/linkaccount"));
    public static final LinkVerificationException LINK_REQUEST_EXPIRED =
            new LinkVerificationException(Message.LINK_REQUEST_EXPIRED, literal("command", "/linkaccount"));

    private final TranslatableMessage message;
    private final Placeholder[] placeholders;

    private LinkVerificationException(TranslatableMessage message, Placeholder... placeholders) {
        super(null, null, true, false);
        this.message = message;
        this.placeholders = placeholders;
    }

    public TranslatableMessage message() {
        return message;
    }

    public Placeholder[] placeholders() {
        return placeholders;
    }
}
