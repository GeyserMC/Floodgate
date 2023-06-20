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

package org.geysermc.floodgate.core.api.legacy;

import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.core.player.FloodgateConnection;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.UiProfile;

public class LegacyPlayerWrapper implements FloodgatePlayer {
    private final FloodgateConnection connection;

    public LegacyPlayerWrapper(FloodgateConnection connection) {
        this.connection = connection;
    }

    @Override
    public String getJavaUsername() {
        throw new UnsupportedOperationException(); //todo
    }

    @Override
    public UUID getJavaUniqueId() {
        throw new UnsupportedOperationException(); //todo
    }

    @Override
    public UUID getCorrectUniqueId() {
        return connection.javaUuid();
    }

    @Override
    public String getCorrectUsername() {
        return connection.javaUsername();
    }

    @Override
    public String getVersion() {
        return connection.version();
    }

    @Override
    public String getUsername() {
        return connection.bedrockUsername();
    }

    @Override
    public String getXuid() {
        return connection.xuid();
    }

    @Override
    public DeviceOs getDeviceOs() {
        return DeviceOs.fromId(connection.platform().ordinal());
    }

    @Override
    public String getLanguageCode() {
        return connection.languageCode();
    }

    @Override
    public UiProfile getUiProfile() {
        return UiProfile.fromId(connection.uiProfile().ordinal());
    }

    @Override
    public InputMode getInputMode() {
        return InputMode.fromId(connection.inputMode().ordinal());
    }

    @Override
    public boolean isFromProxy() {
        throw new UnsupportedOperationException(); //todo
    }

    @Override
    public LinkedPlayer getLinkedPlayer() {
        if (isLinked()) {
            return LinkedPlayer.of(
                    connection.javaUsername(), connection.javaUuid(),
                    FloodgateApi.getInstance().createJavaPlayerId(Long.parseLong(getXuid()))
            );
        }
        return null;
    }

    @Override
    public boolean isLinked() {
        return connection.isLinked();
    }

    @Override
    public boolean hasProperty(PropertyKey key) {
        return connection.propertyGlue().hasProperty(key);
    }

    @Override
    public boolean hasProperty(String key) {
        return connection.propertyGlue().hasProperty(key);
    }

    @Override
    public <T> T getProperty(PropertyKey key) {
        return connection.propertyGlue().getProperty(key);
    }

    @Override
    public <T> T getProperty(String key) {
        return connection.propertyGlue().getProperty(key);
    }

    @Override
    public <T> T removeProperty(PropertyKey key) {
        return connection.propertyGlue().removeProperty(key);
    }

    @Override
    public <T> T removeProperty(String key) {
        return connection.propertyGlue().removeProperty(key);
    }

    @Override
    public <T> T addProperty(PropertyKey key, Object value) {
        return connection.propertyGlue().addProperty(key, value);
    }

    @Override
    public <T> T addProperty(String key, Object value) {
        return connection.propertyGlue().addProperty(key, value);
    }
}
