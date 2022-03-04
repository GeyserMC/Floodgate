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

package com.minekube.connect.api.player;

import java.util.UUID;

public interface ConnectPlayer {
    /**
     * Returns the Minecraft uuid of that player.
     */
    UUID getUniqueId();

    /**
     * Returns the Minecraft username of that player.
     */
    String getUsername();

    /**
     * Returns the game profile of that player.
     */
    GameProfile getGameProfile();

    /**
     * Returns the language tag of the player. If it is unknown it returns an empty string.
     */
    String getLanguageTag();

    /**
     * Returns the session id sent by Connect WatchService.
     */
    String getSessionId();

    /**
     * Returns the authentication mode of the player connection.
     */
    Auth getAuth();

    /**
     * Casts the ConnectPlayer instance to a class that extends ConnectPlayer.
     *
     * @param <T> The instance to cast to.
     * @return The ConnectPlayer casted to the given class
     * @throws ClassCastException when it can't cast the instance to the given class
     */
    default <T extends ConnectPlayer> T as(Class<T> clazz) {
        return clazz.cast(this);
    }
}
