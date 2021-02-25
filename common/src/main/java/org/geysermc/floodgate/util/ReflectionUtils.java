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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

public final class ReflectionUtils {
    /**
     * The package name that is shared between all the {@link #getPrefixedClass(String)} calls so
     * that the className will be a lot shorter. Example net.minecraft.server.v1_8R3.PacketHandshakingInSetProtocol
     * will become PacketHandshakingInSetProtocol if the prefix is set to
     * net.minecraft.server.v1_8R3
     */
    @Getter
    @Setter
    private static String prefix;

    /**
     * Get a class that is prefixed with the prefix provided in {@link #setPrefix(String)}. Calling
     * this method is equal to calling {@link #getClass(String)} with <i>prefix</i>.<i>classname</i>
     * as class name.
     *
     * @param className the prefix class to find
     * @return the class if found, otherwise null
     */
    @Nullable
    public static Class<?> getPrefixedClass(String className) {
        return getClass(prefix + "." + className);
    }

    /**
     * Get the class from a class name. Calling this method is equal to calling {@link
     * Class#forName(String)} where String is the class name.<br> This method will return null when
     * the class isn't found instead of throwing the exception, but the exception will be printed to
     * the console.
     *
     * @param className the name of the class to find
     * @return the class or null if the class wasn't found.
     */
    @Nullable
    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameters) {
        try {
            return clazz.getConstructor(parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    public static Object newInstance(Constructor<?> constructor, Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Get a field of a class. Calling this method is equal to calling {@link
     * Class#getField(String)} where String is the fieldName when isPublic is true and calling this
     * method is equal to calling {@link Class#getDeclaredField(String)} where String is the
     * fieldName when isPublic is false.<br> Please note that this method will return null instead
     * of throwing the exception.
     *
     * @param clazz     the class name to get the field from
     * @param fieldName the name of the field
     * @param declared  if the field is declared or public.
     * @return the field if found, otherwise null
     */
    @Nullable
    public static Field getField(Class<?> clazz, String fieldName, boolean declared) {
        try {
            if (declared) {
                return clazz.getField(fieldName);
            }
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    /**
     * Get a field from a class, it doesn't matter if the field is public or not. This method will
     * first try to get a declared field and if that failed it'll try to get a public field.
     *
     * @param clazz     the class to get the field from
     * @param fieldName the name of the field
     * @return the field if found, otherwise null
     */
    @Nullable
    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = getField(clazz, fieldName, true);
        if (field != null) {
            return field;
        }
        return getField(clazz, fieldName, false);
    }

    /**
     * Get a field from a class without having to provide a field name.
     *
     * @param clazz     the class to search the field from
     * @param fieldType the type of the field
     * @param declared  if the field is declared or public
     * @return the field if it has been found, otherwise null
     */
    @Nullable
    public static Field getFieldOfType(Class<?> clazz, Class<?> fieldType, boolean declared) {
        Field[] fields = declared ? clazz.getDeclaredFields() : clazz.getFields();
        for (Field field : fields) {
            makeAccessible(field);
            if (field.getType() == fieldType) {
                return field;
            }
        }
        return null;
    }

    /**
     * Get a declared field from a class without having to provide a field name.<br> Calling this
     * method is equal to calling {@link #getFieldOfType(Class, Class, boolean)} with declared =
     * true.
     *
     * @param clazz     the class to search the field from
     * @param fieldType the type of the declared field
     * @return the field if it has been found, otherwise null
     */
    @Nullable
    public static Field getFieldOfType(Class<?> clazz, Class<?> fieldType) {
        return getFieldOfType(clazz, fieldType, true);
    }

    /**
     * Get the value of a field. This method first makes the field accessible and then gets the
     * value.<br> This method will return null instead of throwing an exception, but it'll log the
     * stacktrace to the console.
     *
     * @param instance the instance to get the value from
     * @param field    the field to get the value from
     * @return the value when succeeded, otherwise null
     */
    @Nullable
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
     * Get the value of the given field by finding the field and then get the value of it.
     *
     * @param instance  the instance of the object
     * @param fieldName the name of the field to get the value from
     * @return the value of the field when succeeded, otheriwse null
     */
    @Nullable
    public static Object getValue(Object instance, String fieldName) {
        return getValue(instance, getField(instance.getClass(), fieldName));
    }

    /**
     * Get the value of a field and cast it to <T>.
     *
     * @param instance the instance to get the value from
     * @param field    the field to get the value from
     * @param <T>      the type to cast the value to
     * @return the casted value when succeeded, otherwise null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T getCastedValue(Object instance, Field field) {
        return (T) getValue(instance, field);
    }

    /**
     * Get the value of a field and cast it to <T>.
     *
     * @param instance  the instance to get the value from
     * @param fieldName the field to get the value from
     * @param <T>       the type to cast the value to
     * @return the casted value when succeeded, otherwise null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T getCastedValue(Object instance, String fieldName) {
        return (T) getValue(instance, getField(instance.getClass(), fieldName));
    }

    /**
     * Set the value of a field. This method make the field accessible and then sets the value.<br>
     * This method doesn't throw an exception when failed, but it'll log the error to the console.
     *
     * @param instance the instance to set the value to
     * @param field    the field to set the value to
     * @param value    the value to set
     */
    public static void setValue(Object instance, Field field, Object value) {
        makeAccessible(field);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set the value of a field. This method finds the field, and then calls {@link
     * #setValue(Object, Field, Object)}.
     *
     * @param instance  the instance to set the value to
     * @param fieldName the field to set the value to
     * @param value     the value to set
     * @return true if the field was found
     */
    public static boolean setValue(Object instance, String fieldName, Object value) {
        Field field = getField(instance.getClass(), fieldName);
        if (field != null) {
            setValue(instance, field, value);
        }
        return field != null;
    }

    /**
     * Get a method from a class, it doesn't matter if the field is public or not. This method will
     * first try to get a declared field and if that failed it'll try to get a public field.<br>
     * Instead of throwing an exception when the method wasn't found, it will return null, but the
     * exception will be printed in the console.
     *
     * @param clazz     the class to get the method from
     * @param method    the name of the method to find
     * @param declared  if the the method is declared or public
     * @param arguments the classes of the method arguments
     * @return the requested method if it has been found, otherwise null
     */
    @Nullable
    public static Method getMethod(
            Class<?> clazz, String method,
            boolean declared,
            Class<?>... arguments) {
        try {
            if (declared) {
                return clazz.getMethod(method, arguments);
            }
            return clazz.getDeclaredMethod(method, arguments);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    /**
     * Get a method from a class, it doesn't matter if the method is public or not. This method will
     * first try to get a declared method and if that fails it'll try to get a public method.
     *
     * @param clazz      the class to get the method from
     * @param methodName the name of the method to find
     * @param arguments  the classes of the method arguments
     * @return the requested method if it has been found, otherwise null
     */
    @Nullable
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... arguments) {
        Method method = getMethod(clazz, methodName, true, arguments);
        if (method != null) {
            return method;
        }
        return getMethod(clazz, methodName, false, arguments);
    }

    /**
     * Get a method from a class, it doesn't matter if the method is public or not. This method will
     * first try to get a declared method and if that fails it'll try to get a public method.
     *
     * @param instance   the class to get the method from
     * @param methodName the name of the method to find
     * @param arguments  the classes of the method arguments
     * @return the requested method if it has been found, otherwise null
     */
    @Nullable
    public static Method getMethod(Object instance, String methodName, Class<?>... arguments) {
        return getMethod(instance.getClass(), methodName, arguments);
    }

    /**
     * Get a method from a class by using the name of the method.
     *
     * @param clazz      the class to search the method in
     * @param methodName the name of the method
     * @param declared   if the method is declared or public
     * @return the method if it has been found, otherwise null
     */
    @Nullable
    public static Method getMethodByName(Class<?> clazz, String methodName, boolean declared) {
        Method[] methods = declared ? clazz.getDeclaredMethods() : clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Get a method from a class without having to provide a method name.
     *
     * @param clazz     the class to search the method in
     * @param paramType the type of one of the method parameters
     * @param declared  if the method is declared or public
     * @return the method if it has been found, otherwise null
     */
    @Nullable
    public static Method getMethodFromParam(Class<?> clazz, Class<?> paramType, boolean declared) {
        Method[] methods = declared ? clazz.getDeclaredMethods() : clazz.getMethods();
        for (Method method : methods) {
            for (Class<?> parameter : method.getParameterTypes()) {
                if (parameter == paramType) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Invoke the given method of the given instance with the given arguments.
     *
     * @param instance  the instance to get the value from
     * @param method    the method to invoke
     * @param arguments the arguments of the method
     * @return the value got from invoking the method, or null when failed to invoke
     */
    @Nullable
    public static Object invoke(Object instance, Method method, Object... arguments) {
        if (method == null) {
            return null;
        }
        makeAccessible(method);
        try {
            return method.invoke(instance, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static <T> T cast(Object instance, Class<T> castTo) {
        if (castTo == null) {
            throw new IllegalArgumentException("Cannot cast instance to null");
        }
        return castTo.cast(instance);
    }

    /**
     * Invoke the given method of the given instance with the given arguments and cast the value.
     *
     * @param instance  the instance to get the value from
     * @param method    the method to invoke
     * @param arguments the arguments of the method
     * @return the casted value got from invoking the method, or null when failed to invoke
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T castedInvoke(Object instance, Method method, Object... arguments) {
        return (T) invoke(instance, method, arguments);
    }

    /**
     * Invoke the given method of the given instance and cast the value.
     *
     * @param instance the instance to get the value from
     * @param method   the method to invoke
     * @return the casted value got from invoking the method, or null when failed to invoke
     */
    @Nullable
    public static <T> T castedInvoke(Object instance, String method) {
        return castedInvoke(instance, getMethod(instance.getClass(), method));
    }

    /**
     * Invoke the given static method.
     *
     * @param clazz  the class to get the method from
     * @param method the name of the method to invoke
     * @return the value got from invoking the status method, or null when failed to invoke
     */
    @Nullable
    public static Object invokeStatic(Class<?> clazz, String method) {
        return invoke(null, getMethod(clazz, method));
    }

    /**
     * Make the object accessible if it isn't accessible yet
     *
     * @param accessibleObject the object to make accessible
     * @param <T>              accessible object type
     * @return the accessibleObject
     */
    public static <T extends AccessibleObject> T makeAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
        return accessibleObject;
    }
}
