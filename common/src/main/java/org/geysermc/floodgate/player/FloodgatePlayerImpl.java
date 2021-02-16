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

package org.geysermc.floodgate.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.api.player.PropertyKey.Result;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.floodgate.util.Utils;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public final class FloodgatePlayerImpl implements FloodgatePlayer {
    private final String version;
    private final String username;
    private final String javaUsername;
    private final UUID javaUniqueId;
    private final String xuid;
    private final DeviceOs deviceOs;
    private final String languageCode;
    private final UiProfile uiProfile;
    private final InputMode inputMode;
    private final String ip;
    private final boolean fromProxy;
    private final boolean proxy; // if current platform is a proxy
    private final LinkedPlayer linkedPlayer;

    private final int subscribeId;
    private final String verifyCode;

    @Getter(AccessLevel.PRIVATE)
    public Map<PropertyKey, Object> propertyKeyToValue;
    @Getter(AccessLevel.PRIVATE)
    private Map<String, PropertyKey> stringToPropertyKey;

    /**
     * Returns true if the player is still logging in
     */
    @Setter private boolean login = true;

    protected static FloodgatePlayerImpl from(
            BedrockData data,
            HandshakeData handshakeData) {

        SimpleFloodgateApi api = InstanceHolder.castApi(SimpleFloodgateApi.class);

        UUID javaUniqueId = Utils.getJavaUuid(data.getXuid());

        DeviceOs deviceOs = DeviceOs.getById(data.getDeviceOs());
        UiProfile uiProfile = UiProfile.getById(data.getUiProfile());
        InputMode inputMode = InputMode.getById(data.getInputMode());

        LinkedPlayer linkedPlayer = handshakeData.getLinkedPlayer();

        return new FloodgatePlayerImpl(
                data.getVersion(), data.getUsername(), handshakeData.getJavaUsername(),
                javaUniqueId, data.getXuid(), deviceOs, data.getLanguageCode(), uiProfile,
                inputMode, data.getIp(), data.isFromProxy(), api instanceof ProxyFloodgateApi,
                linkedPlayer, data.getSubscribeId(), data.getVerifyCode());
    }

    /**
     * Fetch and return the LinkedPlayer object associated to the player if the player is linked.
     * Please note that this method loads the LinkedPlayer synchronously.
     *
     * @return LinkedPlayer or null if the player isn't linked or linking isn't enabled
     * @see #fetchLinkedPlayerAsync(PlayerLink, UUID) for the asynchronously alternative
     */
    public static LinkedPlayer fetchLinkedPlayer(PlayerLink link, UUID javaUniqueId) {
        if (!link.isEnabled()) {
            return null;
        }

        try {
            return link.getLinkedPlayer(javaUniqueId).get();
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch and return the LinkedPlayer object associated to the player if the player is linked.
     *
     * @return a future holding the LinkedPlayer or null if the player isn't linked or when linking
     * isn't enabled
     * @see #fetchLinkedPlayer(PlayerLink, UUID) for the sync version
     */
    public static CompletableFuture<LinkedPlayer> fetchLinkedPlayerAsync(
            PlayerLink link,
            UUID javaUniqueId) {
        return link.isEnabled() ?
                link.getLinkedPlayer(javaUniqueId) :
                CompletableFuture.completedFuture(null);
    }

    @Override
    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }

    @Override
    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    public BedrockData toBedrockData() {
        return BedrockData.of(version, username, xuid, deviceOs.ordinal(), languageCode,
                uiProfile.ordinal(), inputMode.ordinal(), ip, linkedPlayer, proxy, subscribeId,
                verifyCode);
    }

    @Override
    public boolean hasProperty(PropertyKey key) {
        if (propertyKeyToValue == null) {
            return false;
        }
        return propertyKeyToValue.get(key) != null;
    }

    @Override
    public boolean hasProperty(String key) {
        if (stringToPropertyKey == null) {
            return false;
        }
        return hasProperty(stringToPropertyKey.get(key));
    }

    @Override
    public <T> T getProperty(PropertyKey key) {
        if (propertyKeyToValue == null) {
            return null;
        }
        return (T) propertyKeyToValue.get(key);
    }

    @Override
    public <T> T getProperty(String key) {
        if (stringToPropertyKey == null) {
            return null;
        }
        return getProperty(stringToPropertyKey.get(key));
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
