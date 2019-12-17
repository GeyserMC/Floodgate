package org.geysermc.floodgate.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtil {
    /*
     * Used in the Bukkit version
     */
    private static String nmsPackage = null;

    public static Class<?> getNMSClass(String className) {
        return getClass(nmsPackage + className);
    }

    public static void setServerVersion(String serverVersion) {
        nmsPackage = "net.minecraft.server." + serverVersion + ".";
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


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
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCastedValue(Object instance, Field field, Class<T> returnType) {
        return (T) getValue(instance, field);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCastedValue(Object instance, String fieldName, Class<T> returnType) {
        return (T) getValue(instance, getField(instance.getClass(), fieldName));
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
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... args) {
        try {
            return clazz.getMethod(method, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeStatic(Class<?> clazz, String method) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return clazz.getDeclaredMethod(method).invoke(null);
    }

    public static <T extends AccessibleObject> T makeAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) accessibleObject.setAccessible(true);
        return accessibleObject;
    }
}
