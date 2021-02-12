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

package org.geysermc.floodgate.pluginmessage.channel;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.skin.SkinApplier;

public class SkinChannel implements PluginMessageChannel {
    @Inject private FloodgateApi api;
    @Inject private FloodgateConfig config;
    @Inject private SkinApplier skinApplier;

    @Override
    public String getIdentifier() {
        return "floodgate:skin";
    }

    @Override
    public Result handleProxyCall(
            byte[] data,
            UUID targetUuid,
            String targetUsername,
            Identity targetIdentity,
            UUID sourceUuid,
            String sourceUsername,
            Identity sourceIdentity) {

        // we can only get skins from Geyser (client)
        if (sourceIdentity == Identity.PLAYER) {
            Result result = handleServerCall(data, targetUuid, targetUsername);
            // aka translate 'handled' into 'forward' when send-floodgate-data is enabled
            if (!result.isAllowed() && result.getReason() == null) {
                if (config.isProxy() && ((ProxyFloodgateConfig) config).isSendFloodgateData()) {
                    return Result.forward();
                }
            }
            return result;
        }

        // Servers can't send skin data
        if (sourceIdentity == Identity.SERVER) {
            return Result.kick("Got skin data from a server?");
        }
        return Result.handled();
    }

    @Override
    public Result handleServerCall(byte[] data, UUID targetUuid, String targetUsername) {
        FloodgatePlayer floodgatePlayer = api.getPlayer(targetUuid);
        if (floodgatePlayer == null) {
            return Result.kick("Player sent skins data for a non-Floodgate player");
        }

        String message = new String(data, StandardCharsets.UTF_8);

        String[] split = message.split("\0");
        // value and signature
        if (split.length != 2) {
            return Result.kick("Got invalid skin data");
        }

        String value = split[0];
        String signature = split[1];

        JsonObject result = new JsonObject();
        result.addProperty("value", value);
        result.addProperty("signature", signature);

        floodgatePlayer.addProperty(PropertyKey.SKIN_UPLOADED, result);
        skinApplier.applySkin(floodgatePlayer, result);

        return Result.handled();
    }
}
