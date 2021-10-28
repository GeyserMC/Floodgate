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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

public class GeyserDumpUtils {

    private static Class<?> infoHolderClazz;
    private static Method setConfigMethod;
    private static Method setGitPropertiesMethod;

    static {
        try {
            infoHolderClazz = Class.forName("org.geysermc.connector.dump.FloodgateInfoHolder");
            setConfigMethod = infoHolderClazz.getMethod("setConfig", Object.class);
            setGitPropertiesMethod = infoHolderClazz.getMethod("setGitProperties", Properties.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) { }
        // If class is not found, then Geyser isn't installed, which is fine
        // If no method is found then something was probably renamed
    }

    /**
     * Attempts to set the Floodgate config in Geyser's FloodgateInfoHolder
     * Does nothing if Geyser is not present or there was any problem calling the set method.
     * @param config The Floodgate configuration to set
     */
    public static void setConfig(Object config) {
        if (setConfigMethod != null) {
            try {
                setConfigMethod.invoke(infoHolderClazz, config);
            } catch (IllegalAccessException | InvocationTargetException ignored) { }
        }
    }

    /**
     * Attempts to set the Floodgate git.properties in Geyser's FloodgateInfoHolder
     * Does nothing if Geyser is not present or there was any problem calling the set method.
     * @param gitProperties Floodgate's git.properties to set
     */
    public static void setGitProperties(Properties gitProperties) {
        if (setGitPropertiesMethod != null) {
            try {
                setGitPropertiesMethod.invoke(infoHolderClazz, gitProperties);
            } catch (IllegalAccessException | InvocationTargetException ignored) { }
        }
    }
}
