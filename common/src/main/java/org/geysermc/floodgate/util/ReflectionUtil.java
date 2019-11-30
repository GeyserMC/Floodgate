package org.geysermc.floodgate.util;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static Field getField(Class<?> clazz, String fieldName, boolean isPublic) {
        try {
            return isPublic ? clazz.getField(fieldName) : clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = getField(clazz, fieldName, false);
        return field != null ? field : getField(clazz, fieldName, true);
    }

    public static boolean setValue(Object instance, String fieldName, Object value) {
        Field field = getField(instance.getClass(), fieldName);
        if (field != null) {
            setValue(instance, field, value);
        }
        return field != null;
    }

    public static void setValue(Object instance, Field field, Object value) {
        if (!field.isAccessible()) field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
