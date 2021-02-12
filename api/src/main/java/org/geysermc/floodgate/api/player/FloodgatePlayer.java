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

import java.util.UUID;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.UiProfile;

public interface FloodgatePlayer {
    /**
     * Returns the Bedrock username that will be used as username on the server. This includes
     * replace spaces (if enabled), username shortened and prefix appended.<br> Note that this field
     * is not used when the player is a {@link LinkedPlayer LinkedPlayer}
     */
    String getJavaUsername();

    /**
     * Returns the uuid that will be used as UUID on the server.<br> Note that this field is not
     * used when the player is a {@link LinkedPlayer LinkedPlayer}
     */
    UUID getJavaUniqueId();

    /**
     * Returns the uuid that the server will use as uuid of that player. Will return {@link
     * #getJavaUniqueId()} when not linked or {@link LinkedPlayer#getJavaUniqueId()} when linked.
     */
    UUID getCorrectUniqueId();

    /**
     * Returns the username the server will as username for that player. Will return {@link
     * #getJavaUsername()} when not linked or {@link LinkedPlayer#getJavaUsername()} when linked.
     */
    String getCorrectUsername();

    /**
     * Returns the version of the Bedrock client
     */
    String getVersion();

    /**
     * Returns the real username of the Bedrock client. This username doesn't have a prefix, spaces
     * aren't replaced and the username hasn't been shortened.
     */
    String getUsername();

    /**
     * Returns the Xbox Unique Identifier of the Bedrock client
     */
    String getXuid();

    /**
     * Returns the Operating System of the Bedrock client
     */
    DeviceOs getDeviceOs();

    /**
     * Returns the language code of the Bedrock client
     */
    String getLanguageCode();

    /**
     * Returns the User Interface Profile of the Bedrock client
     */
    UiProfile getUiProfile();

    /**
     * Returns the Input Mode of the Bedrock client
     */
    InputMode getInputMode();

    /**
     * Returns if the Floodgate player is connected through a proxy
     */
    boolean isFromProxy();

    /**
     * Returns the LinkedPlayer object if the player is linked to a Java account.
     */
    LinkedPlayer getLinkedPlayer();

    default boolean sendForm(Form form) {
        return FloodgateApi.getInstance().sendForm(getCorrectUniqueId(), form);
    }

    default boolean sendForm(FormBuilder<?, ?> formBuilder) {
        return sendForm(formBuilder.build());
    }

    boolean hasProperty(PropertyKey key);

    boolean hasProperty(String key);

    <T> T getProperty(PropertyKey key);

    <T> T getProperty(String key);

    <T> T removeProperty(PropertyKey key);

    <T> T removeProperty(String key);

    <T> T addProperty(PropertyKey key, Object value);

    <T> T addProperty(String key, Object value);

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
