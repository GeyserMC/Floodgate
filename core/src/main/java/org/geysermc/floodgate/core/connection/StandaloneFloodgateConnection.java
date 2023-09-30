package org.geysermc.floodgate.core.connection;

import java.net.InetAddress;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.api.util.UiProfile;
import org.geysermc.floodgate.util.LinkedPlayer;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class StandaloneFloodgateConnection extends FloodgateConnection {
    private final String version;
    private final String username;
    private final UUID identity;
    private final String xuid;
    private final String javaUsername;
    private final UUID javaUniqueId;
    private final BedrockPlatform deviceOs;
    private final String languageCode;
    private final UiProfile uiProfile;
    private final InputMode inputMode;
    private final InetAddress ip;
    private final LinkedPlayer linkedPlayer;

    @Override
    public @NonNull String bedrockUsername() {
        return username;
    }

    public @NonNull UUID identity() {
        return identity;
    }

    @Override
    public @NonNull String xuid() {
        return xuid;
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
    public @NonNull InetAddress ip() {
        return ip;
    }

    @Override
    public @Nullable LinkedPlayer linkedPlayer() {
        return linkedPlayer;
    }

    public void fillBuilder(FloodgateConnectionBuilder builder) {
        builder.version(version)
                .username(username)
                .identity(identity)
                .xuid(xuid)
                .deviceOs(deviceOs)
                .languageCode(languageCode)
                .uiProfile(uiProfile)
                .inputMode(inputMode)
                .ip(ip)
                .linkedPlayer(linkedPlayer);
    }
}
