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

package org.geysermc.floodgate.core.connection;

import java.net.InetAddress;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.api.Geyser;
import org.geysermc.api.connection.Connection;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.core.api.legacy.LegacyPlayerWrapper;
import org.geysermc.floodgate.core.api.legacy.PropertyGlue;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;

public abstract class FloodgateConnection implements Connection {
    private final PropertyGlue propertyGlue = new PropertyGlue();
    private LegacyPlayerWrapper legacyPlayer;

    public abstract @NonNull UUID identity();

    public abstract @NonNull InetAddress ip();

    public abstract @Nullable LinkedPlayer linkedPlayer();

    @Override
    public boolean isLinked() {
        return linkedPlayer() != null;
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

    public abstract @This FloodgateConnection linkedPlayer(@Nullable LinkedPlayer linkedPlayer);

    public BedrockData toBedrockData() {
        return BedrockData.of(
                version(), bedrockUsername(), xuid(), platform().ordinal(), languageCode(), uiProfile().ordinal(),
                inputMode().ordinal(), ip().getHostAddress(), linkedPlayer(), false, 0, null
        );
    }

    public LegacyPlayerWrapper legacySelf() {
        if (legacyPlayer == null) {
            legacyPlayer = new LegacyPlayerWrapper(this, javaUsername(), javaUuid());
        }
        return legacyPlayer;
    }

    public PropertyGlue propertyGlue() {
        return propertyGlue;
    }
}
