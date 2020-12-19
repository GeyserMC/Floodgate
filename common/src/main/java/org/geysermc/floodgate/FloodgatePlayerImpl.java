/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.RawSkin;
import org.geysermc.floodgate.util.UiProfile;

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
    private final LinkedPlayer linkedPlayer;
    private final RawSkin rawSkin;
    @Getter(AccessLevel.PRIVATE)
    public Map<PropertyKey, Object> propertyKeyToValue;
    @Getter(AccessLevel.PRIVATE)
    private Map<String, PropertyKey> stringToPropertyKey;
    /**
     * Returns true if the player is still logging in
     */
    @Setter private boolean login = true;

    protected static FloodgatePlayerImpl from(BedrockData data, RawSkin skin,
                                              FloodgateConfigHolder configHolder) {
        FloodgateApi api = FloodgateApi.getInstance();
        FloodgateConfig config = configHolder.get();

        String prefix = config.getUsernamePrefix();
        int usernameLength = Math.min(data.getUsername().length(), 16 - prefix.length());
        String javaUsername = prefix + data.getUsername().substring(0, usernameLength);
        if (config.isReplaceSpaces()) {
            javaUsername = javaUsername.replaceAll(" ", "_");
        }

        UUID javaUniqueId = api.createJavaPlayerId(Long.parseLong(data.getXuid()));

        DeviceOs deviceOs = DeviceOs.getById(data.getDeviceOs());
        UiProfile uiProfile = UiProfile.getById(data.getUiProfile());
        InputMode inputMode = InputMode.getById(data.getInputMode());

        // RawSkin must be removed from the encrypted data
        if (api instanceof ProxyFloodgateApi) {
            InstanceHolder.castApi(ProxyFloodgateApi.class)
                    .updateEncryptedData(javaUniqueId, data);
        }

        LinkedPlayer linkedPlayer;

        // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
        if (data.hasPlayerLink()) {
            linkedPlayer = data.getLinkedPlayer();
        } else {
            // every implementation (Bukkit, Bungee and Velocity) run this constructor async,
            // so we should be fine doing this synchronised.
            linkedPlayer = fetchLinkedPlayer(api.getPlayerLink(), javaUniqueId);
        }

        FloodgatePlayerImpl player = new FloodgatePlayerImpl(
                data.getVersion(), data.getUsername(), javaUsername, javaUniqueId, data.getXuid(),
                deviceOs, data.getLanguageCode(), uiProfile, inputMode, data.getIp(),
                data.isFromProxy(), linkedPlayer, skin);

        // encrypted data has been changed after fetching the linkedPlayer
        // We have to update it...
        if (linkedPlayer != null && api instanceof ProxyFloodgateApi) {
            InstanceHolder.castApi(ProxyFloodgateApi.class)
                    .updateEncryptedData(player.getCorrectUniqueId(), player.toBedrockData());
        }
        return player;
    }

    /**
     * Fetch and return the LinkedPlayer object associated to the player if the player is linked.
     * Please note that this method loads the LinkedPlayer synchronously.
     *
     * @return LinkedPlayer or null if the player isn't linked or linking isn't enabled
     * @see #fetchLinkedPlayerAsync(PlayerLink, UUID) for the asynchronously alternative
     */
    public static LinkedPlayer fetchLinkedPlayer(PlayerLink link, UUID javaUniqueId) {
        if (!link.isEnabledAndAllowed()) {
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
    public static CompletableFuture<LinkedPlayer> fetchLinkedPlayerAsync(PlayerLink link,
                                                                         UUID javaUniqueId) {
        return link.isEnabledAndAllowed() ?
                link.getLinkedPlayer(javaUniqueId) :
                CompletableFuture.completedFuture(null);
    }

    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }

    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    public BedrockData toBedrockData() {
        return BedrockData.of(
                version, username, xuid, deviceOs.ordinal(), languageCode,
                uiProfile.ordinal(), inputMode.ordinal(), ip, linkedPlayer, fromProxy);
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

        if (propertyKey == null || !propertyKey.isRemoveable()) {
            return null;
        }

        return (T) propertyKeyToValue.remove(propertyKey);
    }

    public <T> T removeProperty(PropertyKey key) {
        if (stringToPropertyKey == null) {
            return null;
        }

        PropertyKey propertyKey = stringToPropertyKey.get(key.getKey());

        if (propertyKey == null || !propertyKey.equals(key) || !propertyKey.isRemoveable()) {
            return null;
        }

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

        if (propertyKey != null && propertyKey.equals(key) && key.isChangeable()) {
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
        if (currentPropertyKey != null && currentPropertyKey.equals(propertyKey)) {
            stringToPropertyKey.put(key, propertyKey);
            return (T) propertyKeyToValue.put(propertyKey, value);
        }

        return (T) stringToPropertyKey.computeIfAbsent(key, (keyString) -> {
            propertyKeyToValue.put(propertyKey, value);
            return propertyKey;
        });
    }
}
