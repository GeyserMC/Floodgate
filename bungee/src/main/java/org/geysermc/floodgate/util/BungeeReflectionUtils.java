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

import com.google.common.base.Preconditions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Reflection for just Bungee specific reflection because Bungee doesn't like accessibility
public class BungeeReflectionUtils {
    private static final Field MODIFIERS_FIELD;

    static {
        Field modifiersField;
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException ignored) {
            modifiersField = fixJava12Support();
        }

        Preconditions.checkNotNull(modifiersField, "Modifiers field cannot be null!");
        MODIFIERS_FIELD = modifiersField;
    }

    private static Field fixJava12Support() {
        // Java 12 compatibility, thanks to https://github.com/powermock/powermock/pull/1010
        try {
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
        } catch (Exception exception) {
            throw new RuntimeException(format(
                    "Cannot find the modifiers field :/\nJava version: {}\nVendor: {} ({})",
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.getProperty("java.vendor.url")
            ), exception);
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
