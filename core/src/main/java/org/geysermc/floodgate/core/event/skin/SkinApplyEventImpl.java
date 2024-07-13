/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.event.skin;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.event.util.AbstractCancellable;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class SkinApplyEventImpl extends AbstractCancellable implements SkinApplyEvent {
    private final FloodgatePlayer player;
    private final SkinData currentSkin;
    private SkinData newSkin;

    public SkinApplyEventImpl(
            @NonNull FloodgatePlayer player,
            @Nullable SkinData currentSkin,
            @NonNull SkinData newSkin
    ) {
        this.player = Objects.requireNonNull(player);
        this.currentSkin = currentSkin;
        this.newSkin = Objects.requireNonNull(newSkin);
    }

    @Override
    public @NonNull FloodgatePlayer player() {
        return player;
    }

    public @Nullable SkinData currentSkin() {
        return currentSkin;
    }

    public @NonNull SkinData newSkin() {
        return newSkin;
    }

    public SkinApplyEventImpl newSkin(@NonNull SkinData skinData) {
        this.newSkin = Objects.requireNonNull(skinData);
        return this;
    }
}
