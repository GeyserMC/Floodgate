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

package org.geysermc.floodgate.skin;

import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.util.RawSkin;

@RequiredArgsConstructor
public class SkinHandler {
    private final SkinUploader uploader = new SkinUploader();
    private final SkinApplier skinApplier;
    private final FloodgateLogger logger;

    public final void handleSkinUploadFor(FloodgatePlayer player,
                                          BiConsumer<Boolean, String> consumer) {
        handleSkinUploadFor(player, player.getRawSkin(), consumer);
    }

    public final void handleSkinUploadFor(FloodgatePlayer player,
                                          RawSkin rawSkin,
                                          BiConsumer<Boolean, String> consumer) {
        if (player == null || rawSkin == null) {
            if (consumer != null) {
                consumer.accept(true, "Skin or Player is null");
            }
            return;
        }

        uploader.uploadSkin(rawSkin)
                .whenComplete((uploadResult, throwable) -> {
                    if (throwable != null) {
                        logger.error(
                                "Failed to upload player skin for " + player.getCorrectUsername(),
                                throwable);

                        if (consumer != null) {
                            consumer.accept(true, throwable.getMessage());
                        }
                        return;
                    }

                    if (uploadResult.getError() != null) {
                        logger.error("Error while uploading player skin for {}: {}",
                                player.getCorrectUsername(), uploadResult.getError());

                        if (consumer != null) {
                            consumer.accept(true, uploadResult.getError());
                        }
                        return;
                    }

                    logger.info("Skin upload successful for " + player.getCorrectUsername());

                    if (consumer != null) {
                        consumer.accept(false, uploadResult.getResponse().toString());
                    }
                    player.addProperty(PropertyKey.SKIN_UPLOADED, uploadResult.getResponse());

                    skinApplier.applySkin(player, uploadResult);
                });
    }
}
