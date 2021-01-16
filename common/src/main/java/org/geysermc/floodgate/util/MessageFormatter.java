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

package org.geysermc.floodgate.util;

public final class MessageFormatter {
    private static final String DELIM_STR = "{}";
    private static final int DELIM_LENGTH = DELIM_STR.length();

    public static String format(String message, Object... arguments) {
        // simple variant of slf4j's parameters.
        if (arguments == null || arguments.length == 0) {
            return message;
        }

        String[] args = new String[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].toString();
        }

        int previousIndex = -1;
        int currentIndex;
        StringBuilder stringBuilder =
                new StringBuilder(message.length() + getArgsContentLength(args));

        for (String argument : args) {
            currentIndex = message.indexOf(DELIM_STR, previousIndex);
            if (currentIndex == -1) {
                // no parameter places left in message,
                // we'll ignore the remaining parameters and return the message
                if (previousIndex == -1) {
                    return message;
                } else {
                    stringBuilder.append(message.substring(previousIndex));
                    return stringBuilder.toString();
                }
            }

            if (previousIndex == -1) {
                stringBuilder.append(message, 0, currentIndex);
            } else {
                stringBuilder.append(message, previousIndex, currentIndex);
            }
            stringBuilder.append(argument);

            // we finished this argument, so we're past the current delimiter
            previousIndex = currentIndex + DELIM_LENGTH;
        }

        if (previousIndex != message.length()) {
            stringBuilder.append(message, previousIndex, message.length());
        }
        return stringBuilder.toString();
    }

    public static int getArgsContentLength(String... args) {
        int length = 0;
        for (String arg : args) {
            length += arg.length();
        }
        return length;
    }
}
