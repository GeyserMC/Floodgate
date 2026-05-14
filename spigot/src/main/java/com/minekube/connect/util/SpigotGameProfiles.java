/*
 * Copyright (c) 2021-2022 Minekube. https://minekube.com
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
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class SpigotGameProfiles {
    private SpigotGameProfiles() {
    }

    public static GameProfile fromConnectProfile(com.minekube.connect.api.player.GameProfile connectProfile) {
        GameProfile profile = newGameProfile(connectProfile);
        if (profile != null) {
            return profile;
        }

        profile = new GameProfile(connectProfile.getUniqueId(), connectProfile.getUsername());
        Object properties = properties(profile);
        for (com.minekube.connect.api.player.GameProfile.Property property : connectProfile.getProperties()) {
            addProperty(properties, property.getName(), authlibProperty(property));
        }
        return profile;
    }

    private static Property authlibProperty(com.minekube.connect.api.player.GameProfile.Property property) {
        String signature = property.getSignature();
        return signature == null || signature.isEmpty()
                ? new Property(property.getName(), property.getValue())
                : new Property(property.getName(), property.getValue(), signature);
    }

    private static GameProfile newGameProfile(com.minekube.connect.api.player.GameProfile connectProfile) {
        try {
            Class<?> propertyMapClass = Class.forName(
                    "com.mojang.authlib.properties.PropertyMap", false, GameProfile.class.getClassLoader());
            Constructor<GameProfile> gameProfileConstructor =
                    GameProfile.class.getConstructor(UUID.class, String.class, propertyMapClass);
            String guavaPackage = String.join(".", "com", "google", "common", "collect") + ".";
            ClassLoader classLoader = propertyMapClass.getClassLoader();
            Class<?> multimapClass = Class.forName(guavaPackage + "Multimap", false, classLoader);
            Class<?> hashMultimapClass = Class.forName(guavaPackage + "HashMultimap", false, classLoader);
            Object multimap = hashMultimapClass.getMethod("create").invoke(null);
            for (com.minekube.connect.api.player.GameProfile.Property property : connectProfile.getProperties()) {
                addProperty(multimap, property.getName(), authlibProperty(property));
            }

            Object propertyMap = propertyMapClass.getConstructor(multimapClass).newInstance(multimap);
            return gameProfileConstructor.newInstance(
                    connectProfile.getUniqueId(), connectProfile.getUsername(), propertyMap);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create GameProfile with properties", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to create GameProfile properties", e);
        }
    }

    private static Object properties(GameProfile profile) {
        try {
            Method properties = GameProfile.class.getMethod("properties");
            return properties.invoke(profile);
        } catch (NoSuchMethodException ignored) {
            try {
                Method getProperties = GameProfile.class.getMethod("getProperties");
                return getProperties.invoke(profile);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to get GameProfile properties", e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get GameProfile properties", e);
        }
    }

    private static void addProperty(Object properties, String name, Property property) {
        try {
            Method put = properties.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(properties, name, property);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add GameProfile property " + name, e);
        }
    }
}
