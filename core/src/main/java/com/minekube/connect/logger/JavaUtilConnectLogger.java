/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package com.minekube.connect.logger;

import static com.minekube.connect.util.MessageFormatter.format;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.util.LanguageManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class JavaUtilConnectLogger implements ConnectLogger {
    private final Logger logger;
    private final LanguageManager languageManager;
    private Level originLevel;

    @Override
    public void error(String message, Object... args) {
        logger.severe(format(message, args));
    }

    @Override
    public void error(String message, Throwable throwable, Object... args) {
        logger.log(Level.SEVERE, format(message, args), throwable);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warning(format(message, args));
    }

    @Override
    public void info(String message, Object... args) {
        logger.info(format(message, args));
    }

    @Override
    public void translatedInfo(String message, Object... args) {
        logger.info(languageManager.getLogString(message, args));
    }

    @Override
    public void debug(String message, Object... args) {
        logger.fine(format(message, args));
    }

    @Override
    public void trace(String message, Object... args) {
        logger.finer(format(message, args));
    }

    @Override
    public void enableDebug() {
        originLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
    }

    @Override
    public void disableDebug() {
        if (originLevel != null) {
            logger.setLevel(originLevel);
        }
    }

    @Override
    public boolean isDebug() {
        return logger.getLevel() == Level.ALL;
    }
}
