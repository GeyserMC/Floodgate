/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import lombok.Setter;

import java.lang.reflect.*;

public final class ReflectionUtil {
    /**
     * Prefix without dot<br>
     * Example net.minecraft.server.v1_8R3.PacketHandhakingInSetProtocol will become:<br>
     *     net.minecraft.server.v1_8R3
     */
    @Setter private static String prefix = null;

    private static Field modifiersField = null;

    public static Class<?> getPrefixedClass(String className) {
        return getClass(prefix + "." + className);
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
            // class is not found, so we return null
            return null;
        }
    }


    public static Field getField(Class<?> clazz, String fieldName, boolean isPublic) {
        try {
            if (isPublic) {
                return clazz.getField(fieldName);
            }
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            // field is not found, so we return null
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = getField(clazz, fieldName, false);
        if (field != null) {
            return field;
        }
        return getField(clazz, fieldName, true);
    }

    public static Field getFieldOfType(Class<?> clazz, Class<?> fieldType) {
        return getFieldOfType(clazz, fieldType, true);
    }

    public static Field getFieldOfType(Class<?> clazz, Class<?> fieldType, boolean declared) {
        Field[] fields = declared ? clazz.getDeclaredFields() : clazz.getFields();
        for (Field field : fields) {
            makeAccessible(field);
            if (field.getType() == fieldType) return field;
        }
        return null;
    }

    public static Object getValue(Object instance, Field field) {
        makeAccessible(field);
        try {
            return field.get(instance);
        } catch (IllegalArgumentException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * This method is equal to running:<br>
     * <code>{@link #getValue(Object, Field) getValue}(instance,
     * {@link #getField(Class, String) getField}(instance.getClass(), fieldName))</code>
     */
    public static Object getValue(Object instance, String fieldName) {
        return getValue(instance, getField(instance.getClass(), fieldName));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCastedValue(Object instance, Field field) {
        return (T) getValue(instance, field);
    }

    /**
     * This method is equal to running:<br>
     * <code>{@link #getCastedValue(Object, Field) getCastedValue}(instance,
     * {@link #getField(Class, String) getField}(instance.getClass(), fieldName))</code>
     */
    public static <T> T getCastedValue(Object instance, String fieldName) {
        return getCastedValue(instance, getField(instance.getClass(), fieldName));
    }

    public static boolean setValue(Object instance, String fieldName, Object value) {
        Field field = getField(instance.getClass(), fieldName);
        if (field != null) {
            setValue(instance, field, value);
        }
        return field != null;
    }

    public static void setValue(Object instance, Field field, Object value) {
        makeAccessible(field);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean setFinalValue(Object instance, Field field, Object value) {
        try {
            makeAccessible(field);

            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers)) {
                Field modifiersField = getModifiersField();
                makeAccessible(modifiersField);
                modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
            }
            setValue(instance, field, value);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static Field getModifiersField(){
        if (modifiersField != null) return modifiersField;

        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException exception) {
            // Java 12 compatibility, thanks to https://github.com/powermock/powermock/pull/1010
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod(
                        "getDeclaredFields0", boolean.class
                );
                makeAccessible(getDeclaredFields0);

                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                for (Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        modifiersField = field;
                        break;
                    }
                }
            } catch (Exception exception1) {
                exception1.printStackTrace();
                return null;
            }
        }
        return modifiersField;
    }

    public static Method getMethod(Class<?> clazz, String method,
                                    boolean isPublic, Class<?>... args) {
        try {
            return isPublic ? clazz.getMethod(method, args) : clazz.getDeclaredMethod(method, args);
        } catch (NoSuchMethodException exception) {
            // method is not found, so we return null
            return null;
        }
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... args) {
        Method method = getMethod(clazz, methodName, false, args);
        if (method != null) {
            return method;
        }
        return getMethod(clazz, methodName, true, args);
    }

    public static Method getMethod(Object instance, String method, Class<?>... args) {
        return getMethod(instance.getClass(), method, args);
    }

    public static Object invoke(Object instance, Method method, Object... args) {
        makeAccessible(method);
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static Object invoke(Object instance, String method) {
        return invoke(instance, getMethod(instance.getClass(), method));
    }

    @SuppressWarnings("unchecked")
    public static <T> T castedInvoke(Object instance, Method method, Object... args) {
        try {
            return (T) invoke(instance, method, args);
        } catch (NullPointerException exception) {
            return null;
        }
    }

    public static <T> T castedInvoke(Object instance, String method) {
        return castedInvoke(instance, getMethod(instance.getClass(), method));
    }

    public static Object invokeStatic(Class<?> clazz, String method) {
        try {
            return invoke(null, getMethod(clazz, method));
        } catch (NullPointerException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static <T extends AccessibleObject> T makeAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
        return accessibleObject;
    }
}
