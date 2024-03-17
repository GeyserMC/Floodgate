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

package org.geysermc.floodgate.core.platform.command;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.util.LanguageManager;

/**
 * TranslatableMessage is the common class for a message that can be translated
 */
public class TranslatableMessage {
    private final String rawMessage;
    private final String[] translateParts;
    private final MessageType type;

    public TranslatableMessage(String rawMessage, MessageType type) {
        this.rawMessage = rawMessage;
        this.translateParts = rawMessage.split(" ");
        this.type = type;
    }

    public TranslatableMessage(String rawMessage) {
        this(rawMessage, MessageType.NONE);
    }

    public Component translateMessage(LanguageManager manager, @Nullable String locale, Placeholder... placeholders) {
        if (locale == null) {
            locale = manager.getDefaultLocale();
        }

        if (translateParts.length == 1) {
            return manager.getString(rawMessage, locale, type, placeholders);
        }

        Component complete = Component.empty();
        for (int i = 0; i < translateParts.length; i++) {
            if (i != 0) {
                complete = complete.append(Component.text(' '));
            }
            complete = complete.append(manager.getString(translateParts[i], locale, type, placeholders));
        }
        return complete;
    }

    @Override
    public String toString() {
        return rawMessage;
    }
}
