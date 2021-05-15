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

import static org.geysermc.floodgate.util.MessageFormatter.format;
import static org.geysermc.floodgate.util.ReflectionUtils.castedInvoke;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.util.ReflectionUtils.makeAccessible;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

// Reflection for just Bungee specific reflection because Bungee doesn't like accessibility :)
public class BungeeReflectionUtils {
    private static final Field MODIFIERS_FIELD;
    private static final Unsafe UNSAFE;

    static {
        Field modifiersField = null; // Should not be null pre-Java-16
        Unsafe unsafe = null; // Should not be null post-Java-16
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException ignored) {
            try {
                modifiersField = fixJava12Support();
            } catch (Exception e) {
                if (Constants.DEBUG_MODE) {
                    e.printStackTrace();
                }

                // At this point, we're probably using Java 16
                try {
                    Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                    unsafeField.setAccessible(true);
                    unsafe = (Unsafe) unsafeField.get(null);
                } catch (Exception exception) {
                    throw new RuntimeException(format(
                        "Cannot initialize required reflection setup :/\nJava version: {}\nVendor: {} ({})",
                        System.getProperty("java.version"),
                        System.getProperty("java.vendor"),
                        System.getProperty("java.vendor.url")
                    ), exception);
                }
            }
        }

        MODIFIERS_FIELD = modifiersField;
        UNSAFE = unsafe;
    }

    private static Field fixJava12Support() throws Exception {
        // Java 12 compatibility, thanks to https://github.com/powermock/powermock/pull/1010
        Method declaredFields = getMethod(Class.class, "getDeclaredFields0", boolean.class);
        if (declaredFields == null) {
            throw new NoSuchMethodException("Cannot find method getDeclaredFields0");
        }

        Field[] fields = castedInvoke(Field.class, declaredFields, false);
        if (fields == null) {
            throw new RuntimeException("The Field class cannot have null fields");
        }

        for (Field field : fields) {
            if ("modifiers".equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    public static boolean isJava16() {
        return UNSAFE != null;
    }

    public static void setJava16Field(Object object, Field field, Object result) {
        try {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
            if (isStatic) {
                UNSAFE.putObject(UNSAFE.staticFieldBase(field), offset, result);
            } else {
                UNSAFE.putObject(object, offset, result);
            }
        } catch (Exception e) {
            throw new RuntimeException(format(
                    "Cannot initialize required reflection setup :/\nJava version: {}\nVendor: {} ({})",
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.getProperty("java.vendor.url"), e));
        }
    }

    /**
     * Remove the final modifier of a specific field
     *
     * @param field the field to remove the final modifier of
     * @return true if succeeded
     */
    public static boolean removeFinal(Field field) {
        try {
            makeAccessible(field);

            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers)) {
                makeAccessible(MODIFIERS_FIELD);
                MODIFIERS_FIELD.setInt(field, modifiers & ~Modifier.FINAL);
            }
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
