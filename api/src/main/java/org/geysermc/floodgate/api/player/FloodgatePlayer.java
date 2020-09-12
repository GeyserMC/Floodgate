package org.geysermc.floodgate.api.player;

import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.UiProfile;

import java.util.UUID;

public interface FloodgatePlayer {
    /**
     * Returns the Bedrock username that will be used as username on the server.
     * This includes replace spaces (if enabled), username shortened and prefix appended.<br>
     * Note that this field is not used when the player is a {@link LinkedPlayer LinkedPlayer}
     */
    String getJavaUsername();

    /**
     * Returns the uuid that will be used as UUID on the server.<br>
     * Note that this field is not used when the player is a {@link LinkedPlayer LinkedPlayer}
     */
    UUID getJavaUniqueId();

    /**
     * Returns the uuid that the server will use as uuid of that player.
     * Will return {@link #getJavaUniqueId()} when not linked or
     * {@link LinkedPlayer#getJavaUniqueId()} when linked.
     */
    UUID getCorrectUniqueId();

    /**
     * Returns the username the server will as username for that player.
     * Will return {@link #getJavaUsername()} when not linked or
     * {@link LinkedPlayer#getJavaUsername()} when linked.
     */
    String getCorrectUsername();

    /**
     * Returns the version of the Bedrock client
     */
    String getVersion();

    /**
     * Returns the real username of the Bedrock client.
     * This username doesn't have a prefix, spaces aren't replaced and the username hasn't been
     * shortened.
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
     * Returns the LinkedPlayer object if the player is linked to a Java account.
     */
    LinkedPlayer getLinkedPlayer();

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
