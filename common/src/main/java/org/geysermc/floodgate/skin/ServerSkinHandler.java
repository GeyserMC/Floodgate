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

import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.util.RawSkin;

public final class ServerSkinHandler extends SkinHandler {
    private final PluginMessageHandler pluginMessageHandler;

    public ServerSkinHandler(SkinApplier skinApplier,
                             FloodgateLogger logger,
                             PluginMessageHandler pluginMessageHandler) {
        super(skinApplier, logger);
        this.pluginMessageHandler = pluginMessageHandler;
    }

    public void handleSkinUploadFor(FloodgatePlayer player) {
        handleSkinUploadFor(player, player.getRawSkin());
    }

    public void handleSkinUploadFor(FloodgatePlayer player, RawSkin rawSkin) {
        if (player == null || rawSkin == null) {
            return;
        }

        handleSkinUploadFor(player, rawSkin,
                (failed, response) -> {
                    if (player.isFromProxy()) {
                        pluginMessageHandler.sendSkinResponse(
                                player.getCorrectUniqueId(),
                                failed,
                                response
                        );
                    }
                });
    }
}
