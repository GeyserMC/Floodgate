/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.api.legacy;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.api.player.PropertyKey.Result;

@SuppressWarnings("unchecked")
public class PropertyGlue {
    @Getter(AccessLevel.PRIVATE)
    private Map<PropertyKey, Object> propertyKeyToValue;
    @Getter(AccessLevel.PRIVATE)
    private Map<String, PropertyKey> stringToPropertyKey;

    public boolean hasProperty(PropertyKey key) {
        if (propertyKeyToValue == null) {
            return false;
        }
        return propertyKeyToValue.get(key) != null;
    }

    public boolean hasProperty(String key) {
        if (stringToPropertyKey == null) {
            return false;
        }
        return hasProperty(stringToPropertyKey.get(key));
    }

    public <T> T getProperty(PropertyKey key) {
        if (propertyKeyToValue == null) {
            return null;
        }
        return (T) propertyKeyToValue.get(key);
    }

    public <T> T getProperty(String key) {
        if (stringToPropertyKey == null) {
            return null;
        }
        return getProperty(stringToPropertyKey.get(key));
    }

    public <T> T removeProperty(String key) {
        if (stringToPropertyKey == null) {
            return null;
        }

        PropertyKey propertyKey = stringToPropertyKey.get(key);

        if (propertyKey == null || !propertyKey.isRemovable()) {
            return null;
        }

        return (T) propertyKeyToValue.remove(propertyKey);
    }

    public <T> T removeProperty(PropertyKey key) {
        if (stringToPropertyKey == null) {
            return null;
        }

        PropertyKey propertyKey = stringToPropertyKey.get(key.getKey());

        if (propertyKey == null || !propertyKey.equals(key) || !propertyKey.isRemovable()) {
            return null;
        }

        stringToPropertyKey.remove(key.getKey());

        return (T) propertyKeyToValue.remove(key);
    }

    public <T> T addProperty(PropertyKey key, Object value) {
        if (stringToPropertyKey == null) {
            stringToPropertyKey = new HashMap<>();
            propertyKeyToValue = new HashMap<>();

            stringToPropertyKey.put(key.getKey(), key);
            propertyKeyToValue.put(key, value);
            return null;
        }

        PropertyKey propertyKey = stringToPropertyKey.get(key.getKey());

        if (propertyKey != null && propertyKey.isAddAllowed(key) == Result.ALLOWED) {
            stringToPropertyKey.put(key.getKey(), key);
            return (T) propertyKeyToValue.put(key, value);
        }

        return (T) stringToPropertyKey.computeIfAbsent(key.getKey(), (keyString) -> {
            propertyKeyToValue.put(key, value);
            return key;
        });
    }

    public <T> T addProperty(String key, Object value) {
        PropertyKey propertyKey = new PropertyKey(key, true, true);

        if (stringToPropertyKey == null) {
            stringToPropertyKey = new HashMap<>();
            propertyKeyToValue = new HashMap<>();

            stringToPropertyKey.put(key, propertyKey);
            propertyKeyToValue.put(propertyKey, value);
            return null;
        }

        PropertyKey currentPropertyKey = stringToPropertyKey.get(key);

        // key is always changeable if it passes this if statement
        if (currentPropertyKey != null && currentPropertyKey.isAddAllowed(key) == Result.ALLOWED) {
            stringToPropertyKey.put(key, propertyKey);
            return (T) propertyKeyToValue.put(propertyKey, value);
        }

        return (T) stringToPropertyKey.computeIfAbsent(key, (keyString) -> {
            propertyKeyToValue.put(propertyKey, value);
            return propertyKey;
        });
    }
}