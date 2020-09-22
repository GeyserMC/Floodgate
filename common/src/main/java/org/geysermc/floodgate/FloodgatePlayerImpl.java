/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate;

import lombok.Getter;
import lombok.Setter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
public final class FloodgatePlayerImpl implements FloodgatePlayer {
    private final String version;
    private final String username;
    private final String javaUsername;
    //todo maybe add a map for platform specific things
    private final UUID javaUniqueId;
    private final String xuid;
    private final DeviceOs deviceOs;
    private final String languageCode;
    private final UiProfile uiProfile;
    private final InputMode inputMode;
    private final String ip;
    private final LinkedPlayer linkedPlayer;
    private final RawSkin rawSkin;

    /**
     * Returns true if the player is still logging in
     */
    @Setter
    private boolean login = true;

    FloodgatePlayerImpl(BedrockData data, RawSkin skin, String prefix, boolean replaceSpaces) {
        FloodgateApi api = FloodgateApi.getInstance();
        version = data.getVersion();
        username = data.getUsername();

        int usernameLength = Math.min(data.getUsername().length(), 16 - prefix.length());
        String editedUsername = prefix + data.getUsername().substring(0, usernameLength);

        if (replaceSpaces) {
            editedUsername = editedUsername.replaceAll(" ", "_");
        }
        javaUsername = editedUsername;
        javaUniqueId = api.createJavaPlayerId(Long.parseLong(data.getXuid()));

        xuid = data.getXuid();
        deviceOs = DeviceOs.getById(data.getDeviceOs());
        languageCode = data.getLanguageCode();
        uiProfile = UiProfile.getById(data.getUiProfile());
        inputMode = InputMode.getById(data.getInputMode());
        ip = data.getIp();
        rawSkin = skin;

        // we'll use the LinkedPlayer provided by Bungee or Velocity (if they included one)
        if (data.hasPlayerLink()) {
            linkedPlayer = data.getLinkedPlayer();
            return;
        }

        // every implementation (Bukkit, Bungee and Velocity) run this constructor async,
        // so we should be fine doing this synchronised.
        linkedPlayer = fetchLinkedPlayer(api.getPlayerLink());

        // oh oh, now our encrypted data is incorrect. We have to update it...
        if (linkedPlayer != null && api instanceof ProxyFloodgateApi) {
            InstanceHolder.castApi(ProxyFloodgateApi.class)
                    .updateEncryptedData(getCorrectUniqueId(), toBedrockData());
        }
    }

    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }

    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    /**
     * Fetch and return the LinkedPlayer object associated to the player if the player is linked.
     * Please note that this method loads the LinkedPlayer synchronously.
     *
     * @return LinkedPlayer or null if the player isn't linked or linking isn't enabled
     * @see #fetchLinkedPlayerAsync(PlayerLink) for the asynchronously alternative
     */
    public LinkedPlayer fetchLinkedPlayer(PlayerLink link) {
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
     * @return a future holding the LinkedPlayer or null if the player isn't linked or when
     * linking isn't enabled
     * @see #fetchLinkedPlayer(PlayerLink) for the sync version
     */
    public CompletableFuture<LinkedPlayer> fetchLinkedPlayerAsync(PlayerLink link) {
        return link.isEnabledAndAllowed() ?
                link.getLinkedPlayer(javaUniqueId) :
                CompletableFuture.completedFuture(null);
    }

    public BedrockData toBedrockData() {
        return new BedrockData(
                version, username, xuid, deviceOs.ordinal(), languageCode,
                uiProfile.ordinal(), inputMode.ordinal(), ip, linkedPlayer
        );
    }
}
