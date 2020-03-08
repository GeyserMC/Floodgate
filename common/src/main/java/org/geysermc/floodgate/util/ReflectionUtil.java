package org.geysermc.floodgate.util;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.*;

public class ReflectionUtil {
    /**
     * Prefix without dot<br>
     * Example net.minecraft.server.v1_8R3.PacketHandhakingInSetProtocol will become:<br>
     *     net.minecraft.server.v1_8R3
     */
    @Getter @Setter
    private static String prefix = null;

    public static Class<?> getPrefixedClass(String className) {
        return getClass(prefix +"."+ className);
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

    public static Object getValue(Object instance, String fieldName) {
        return getValue(instance, getField(instance.getClass(), fieldName));
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

    public static boolean setFinalValue(Object instance, Field field, Object value) {
        try {
            makeAccessible(field);

            Field modifiersField = null;
            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers)) {
                try {
                    modifiersField = Field.class.getDeclaredField("modifiers");
                } catch (NoSuchFieldException e) {
                    // Java 12 compatibility, thanks to https://github.com/powermock/powermock/pull/1010
                    Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                    makeAccessible(getDeclaredFields0);
                    Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                    for (Field classField : fields) {
                        if ("modifiers".equals(classField.getName())) {
                            modifiersField = classField;
                            break;
                        }
                    }
                }
                assert modifiersField != null;
                makeAccessible(modifiersField);
                modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
            }
            setValue(instance, field, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    public static Object invoke(Object instance, Method method) {
        try {
            return method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeCasted(Object instance, Method method, Class<T> cast) {
        return (T) invoke(instance, method);
    }

    public static Object invokeStatic(Class<?> clazz, String method) {
        try {
            return getMethod(clazz, method).invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T extends AccessibleObject> T makeAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) accessibleObject.setAccessible(true);
        return accessibleObject;
    }
}
