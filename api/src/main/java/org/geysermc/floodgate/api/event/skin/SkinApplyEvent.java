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

package org.geysermc.floodgate.api.event.skin;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.event.Cancellable;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

/**
 * @deprecated The Floodgate API has been deprecated in favor of the GeyserApi, which is shared between Geyser
 * and Floodgate
 */
@Deprecated(forRemoval = true, since = "3.0.0")
public interface SkinApplyEvent extends Cancellable {
    /**
     * Returns the player that will receive the skin.
     */
    @NonNull FloodgatePlayer player();

    /**
     * Returns the skin texture currently applied to the player.
     */
    @Nullable SkinData currentSkin();

    /**
     * Returns the skin texture to be applied to the player.
     */
    @NonNull SkinData newSkin();

    /**
     * Sets the skin texture to be applied to the player
     *
     * @param skinData the skin to apply
     * @return this
     */
    @This SkinApplyEvent newSkin(@NonNull SkinData skinData);

    interface SkinData {
        /**
         * Returns the value of the skin texture.
         */
        @NonNull String value();

        /**
         * Returns the signature of the skin texture.
         */
        @NonNull String signature();
    }
}
