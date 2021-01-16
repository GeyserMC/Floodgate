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

package org.geysermc.floodgate.api.player;

import lombok.Getter;

@Getter
public class PropertyKey {
    /**
     * Socket Address returns the InetSocketAddress of the Bedrock player
     */
    public static final PropertyKey SOCKET_ADDRESS =
            new PropertyKey("socket_address", false, false);

    /**
     * Skin Uploaded returns a JsonObject containing the value and signature of the Skin
     */
    public static final PropertyKey SKIN_UPLOADED =
            new PropertyKey("skin_uploaded", false, false);

    private final String key;
    private final boolean changeable;
    private final boolean removable;

    public PropertyKey(String key, boolean changeable, boolean removable) {
        this.key = key;
        this.changeable = changeable;
        this.removable = removable;
    }

    public Result isAddAllowed(Object obj) { //todo use for add and remove
        if (obj instanceof PropertyKey) {
            PropertyKey propertyKey = (PropertyKey) obj;

            if (key.equals(propertyKey.key)) {
                if ((propertyKey.changeable == changeable || propertyKey.changeable) &&
                        (propertyKey.removable == removable || propertyKey.removable)) {
                    return Result.ALLOWED;
                }
                return Result.INVALID_TAGS;
            }
            return Result.NOT_EQUALS;
        }

        if (obj instanceof String) {
            if (key.equals(obj)) {
                if (changeable) {
                    return Result.ALLOWED;
                }
                return Result.NOT_ALLOWED;
            }
            return Result.INVALID_TAGS;
        }
        return Result.NOT_EQUALS;
    }

    public enum Result {
        NOT_EQUALS,
        INVALID_TAGS,
        NOT_ALLOWED,
        ALLOWED
    }
}
