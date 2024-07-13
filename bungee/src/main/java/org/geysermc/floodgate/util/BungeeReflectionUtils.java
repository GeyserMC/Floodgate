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

package org.geysermc.floodgate.util;

import static org.geysermc.floodgate.core.util.MessageFormatter.format;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// Reflection just for Bungee because Bungee is special :)
public class BungeeReflectionUtils {
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) unsafeField.get(null);
        } catch (Exception exception) {
            throw new RuntimeException(format(
                    "Cannot initialize required reflection setup :/\nJava version: {}\nVendor: {} ({})",
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.getProperty("java.vendor.url")
            ), exception);
        }
    }

    public static void setFieldValue(Object object, Field field, Object result) {
        try {
            boolean isStatic = Modifier.isStatic(field.getModifiers());

            long offset = isStatic ?
                    UNSAFE.staticFieldOffset(field) :
                    UNSAFE.objectFieldOffset(field);

            if (isStatic) {
                UNSAFE.putObject(UNSAFE.staticFieldBase(field), offset, result);
            } else {
                UNSAFE.putObject(object, offset, result);
            }
        } catch (Exception e) {
            throw new RuntimeException(format(
                    "Java version: {}\nVendor: {} ({})",
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.getProperty("java.vendor.url"), e));
        }
    }
}
