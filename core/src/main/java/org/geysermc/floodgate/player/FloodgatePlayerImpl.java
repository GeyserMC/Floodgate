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

package org.geysermc.floodgate.player;

import java.net.InetSocketAddress;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.api.Geyser;
import org.geysermc.api.connection.Connection;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.api.util.UiProfile;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.addon.data.HandshakeDataImpl;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.Utils;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class FloodgatePlayerImpl implements Connection {
    private final String version;
    private final String username;
    private final String javaUsername;
    private final UUID javaUniqueId;
    private final String xuid;
    private final BedrockPlatform deviceOs;
    private final String languageCode;
    private final UiProfile uiProfile;
    private final InputMode inputMode;
    private final String ip;
    private final boolean proxy; // if current platform is a proxy
    private final LinkedPlayer linkedPlayer;

    private final int subscribeId;
    private final String verifyCode;

    private final InetSocketAddress socketAddress;

    static FloodgatePlayerImpl from(BedrockData data, HandshakeDataImpl handshakeData, int port) {
        UUID javaUniqueId = Utils.getJavaUuid(data.getXuid());

        BedrockPlatform deviceOs = BedrockPlatform.fromId(data.getDeviceOs());
        UiProfile uiProfile = UiProfile.fromId(data.getUiProfile());
        InputMode inputMode = InputMode.fromId(data.getInputMode());

        LinkedPlayer linkedPlayer = handshakeData.getLinkedPlayer();

        InetSocketAddress socketAddress = new InetSocketAddress(data.getIp(), port);

        return new FloodgatePlayerImpl(
                data.getVersion(), data.getUsername(), handshakeData.getJavaUsername(),
                javaUniqueId, data.getXuid(), deviceOs, data.getLanguageCode(), uiProfile,
                inputMode, data.getIp(), data.isFromProxy(),
                linkedPlayer, data.getSubscribeId(), data.getVerifyCode(), socketAddress);
    }

    @Override
    public @NonNull String bedrockUsername() {
        return username;
    }

    @Override
    public @MonotonicNonNull String javaUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    @Override
    public @MonotonicNonNull UUID javaUuid() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }

    @Override
    public @NonNull String xuid() {
        return xuid;
    }

    @Override
    public @NonNull String version() {
        return version;
    }

    @Override
    public @NonNull BedrockPlatform platform() {
        return deviceOs;
    }

    @Override
    public @NonNull String languageCode() {
        return languageCode;
    }

    @Override
    public @NonNull UiProfile uiProfile() {
        return uiProfile;
    }

    @Override
    public @NonNull InputMode inputMode() {
        return inputMode;
    }

    @Override
    public boolean isLinked() {
        return linkedPlayer != null;
    }

    @Override
    public boolean sendForm(@NonNull Form form) {
        return Geyser.api().sendForm(javaUuid(), form);
    }

    @Override
    public boolean sendForm(@NonNull FormBuilder<?, ?, ?> formBuilder) {
        return Geyser.api().sendForm(javaUuid(), formBuilder);
    }

    @Override
    public boolean transfer(@NonNull String address, @IntRange(from = 0L, to = 65535L) int port) {
        return Geyser.api().transfer(javaUuid(), address, port);
    }

    @Override
    public InetSocketAddress socketAddress() {
        return socketAddress;
    }

    public BedrockData toBedrockData() {
        return BedrockData.of(version, username, xuid, deviceOs.ordinal(), languageCode,
                uiProfile.ordinal(), inputMode.ordinal(), ip, linkedPlayer, proxy, subscribeId,
                verifyCode);
    }
}
