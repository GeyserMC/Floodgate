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

package com.minekube.connect.api.player;

import java.util.List;
import java.util.UUID;

public interface FloodgatePlayer {
    /**
     * Returns the Minecraft uuid of that player.
     */
    UUID getUniqueId();

    /**
     * Returns the Minecraft username of that player.
     */
    String getUsername();

    /**
     * Returns the game profile properties of that player.
     */
    List<GameProfileProperty> getProperties();

    /**
     * Returns the language code of the player. If it is unknown it returns an empty string.
     */
    String getLanguageCode();

    /**
     * Returns the session id sent by Connect WatchService.
     */
    String getSessionId();

    /**
     * Returns the authentication mode of the player connection.
     */
    Auth getAuth();

    /**
     * Casts the FloodgatePlayer instance to a class that extends FloodgatePlayer.
     *
     * @param <T> The instance to cast to.
     * @return The FloodgatePlayer casted to the given class
     * @throws ClassCastException when it can't cast the instance to the given class
     */
    default <T extends FloodgatePlayer> T as(Class<T> clazz) {
        return clazz.cast(this);
    }
}
