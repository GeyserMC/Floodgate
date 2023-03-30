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

package org.geysermc.floodgate.api.logger;

public interface FloodgateLogger {
    String LOGGER_NAME = "Floodgate";

    /**
     * Logs an error message to the console, with 0 or more arguments.
     *
     * @param message the message to log to the console
     * @param args    the arguments to fill the missing spots in the message
     */
    void error(String message, Object... args);

    /**
     * Logs an error message to the console, with 0 or more arguments.
     *
     * @param message   the message to log to the console
     * @param throwable the throwable to log
     * @param args      the arguments to fill the missing spots in the message
     */
    void error(String message, Throwable throwable, Object... args);

    /**
     * Logs a warning message to the console, with 0 or more arguments.
     *
     * @param message the message to log to the console
     * @param args    the arguments to fill the missing spots in the message
     */
    void warn(String message, Object... args);

    /**
     * Logs an info message to the console, with 0 or more arguments.
     *
     * @param message the message to log to the console
     * @param args    the arguments to fill the missing spots in the message
     */
    void info(String message, Object... args);

    void translatedInfo(String message, Object... args);

    /**
     * Logs a debug message to the console, with 0 or more arguments.
     *
     * @param message the message to log to the console
     * @param args    the arguments to fill the missing spots in the message
     */
    void debug(String message, Object... args);

    /**
     * Logs a trace message to the console, with 0 or more arguments.
     *
     * @param message the message to log to the console
     * @param args    the arguments to fill the missing spots in the message
     */
    void trace(String message, Object... args);

    /**
     * Returns true if debugging is enabled
     */
    boolean isDebug();
}
