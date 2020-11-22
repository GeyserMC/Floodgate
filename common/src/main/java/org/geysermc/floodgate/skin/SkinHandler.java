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

package org.geysermc.floodgate.skin;

import static org.geysermc.floodgate.util.MessageFormatter.format;

import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.skin.SkinUploader.UploadResult;

@RequiredArgsConstructor
public abstract class SkinHandler {
    private final SkinUploader uploader = new SkinUploader();
    private final PluginMessageHandler messageHandler;
    private final FloodgateLogger logger;

    public final void handleSkinUploadFor(FloodgatePlayer player) {
        uploader.uploadSkin(player.getRawSkin())
                .whenComplete((uploadResult, throwable) -> {
                    if (throwable != null) {
                        logger.error(
                                "Failed to upload player skin for " + player.getCorrectUsername(),
                                throwable);

                        messageHandler.sendSkinResponse(
                                player.getJavaUniqueId(), throwable.getMessage());
                        return;
                    }

                    if (uploadResult.getError() != null) {
                        logger.error(format(
                                "Error while uploading player skin for {}: {}",
                                player.getCorrectUsername(), uploadResult.getError()));

                        messageHandler.sendSkinResponse(
                                player.getJavaUniqueId(), uploadResult.getError());
                        return;
                    }

                    logger.info("Skin upload successful for " + player.getCorrectUsername());
                    logger.info(uploadResult.getResponse().toString());
                    messageHandler.sendSkinResponse(
                            player.getJavaUniqueId(), uploadResult.getResponse().toString());
                    applySkin(player, uploadResult);
                });
    }

    protected abstract void applySkin(FloodgatePlayer player, UploadResult result);
}
