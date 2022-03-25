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

package com.minekube.connect.config.loader;

import com.minekube.connect.config.ConnectConfig;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.FieldProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class ConfigInitializer {
    private static final Yaml YAML;

    static {
        Constructor constructor =
                new CustomClassLoaderConstructor(ConfigInitializer.class.getClassLoader());

        constructor.setPropertyUtils(new PropertyUtils() {
            @Override
            protected Map<String, Property> getPropertiesMap(Class<?> type, BeanAccess bAccess) {
                Map<String, Property> properties = new LinkedHashMap<>();
                getPropertiesFromClass(type, ConnectConfig.class, properties);
                return properties;
            }

            private void getPropertiesFromClass(
                    Class<?> type,
                    Class<?> stopAfter,
                    Map<String, Property> propertyMap) {

                Class<?> current = type;
                while (!Object.class.equals(current)) {
                    for (Field field : current.getDeclaredFields()) {
                        int modifiers = field.getModifiers();
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                            String correctName = getCorrectName(field.getName());
                            // children should override parents
                            propertyMap.putIfAbsent(correctName, new FieldProperty(field));
                        }

                        if (field.getClass().getSuperclass().equals(current)) {
                            getPropertiesFromClass(field.getClass(), field.getClass(), propertyMap);
                        }
                    }

                    if (current.equals(stopAfter)) {
                        return;
                    }

                    current = type.getSuperclass();
                }
            }

            private String getCorrectName(String name) {
                // convert sendFloodgateData to send-connect-data,
                // which is the style of writing config fields
                StringBuilder propertyBuilder = new StringBuilder();
                for (int i = 0; i < name.length(); i++) {
                    char current = name.charAt(i);
                    if (Character.isUpperCase(current)) {
                        propertyBuilder.append('-').append(Character.toLowerCase(current));
                    } else {
                        propertyBuilder.append(current);
                    }
                }
                return propertyBuilder.toString();
            }
        });
        constructor.getPropertyUtils().setSkipMissingProperties(true);
        YAML = new Yaml(constructor);
    }

    public static <T extends ConnectConfig> T initializeFrom(
            InputStream dataStream,
            Class<T> configClass) {
        return YAML.loadAs(dataStream, configClass);
    }
}
