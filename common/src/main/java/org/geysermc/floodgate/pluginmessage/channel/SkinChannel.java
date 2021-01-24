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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinHandler;
import org.geysermc.floodgate.skin.SkinUploader.UploadResult;
import org.geysermc.floodgate.util.Base64Utils;
import org.geysermc.floodgate.util.RawSkin;

public class SkinChannel implements PluginMessageChannel {
    private static final Gson GSON = new Gson();

    @Inject private FloodgateApi api;
    @Inject private PluginMessageUtils pluginMessageUtils;
    @Inject private SkinHandler skinHandler;
    @Inject private SkinApplier skinApplier;
    @Inject private FloodgateLogger logger;

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

        if (data.length < 1) {
            return Result.kick("Got invalid Skin request/response");
        }

        boolean request = data[0] == 1;

        if (!request && data.length < 2) {
            return Result.kick("Got invalid Skin response");
        }

        if (sourceIdentity == Identity.SERVER) {
            if (request) {
                return Result.kick("Got Skin request from Server?");
            }

            FloodgatePlayer floodgatePlayer = api.getPlayer(targetUuid);

            if (floodgatePlayer == null) {
                return Result.kick("Server issued Skin request for non-Floodgate player");
            }

            // 1 = failed, 0 = successful.

            // we'll try it again on the next server if it failed
            if (data[1] != 0) {
                return Result.handled();
            }

            // we only have to continue if the player doesn't already have a skin uploaded
            if (floodgatePlayer.hasProperty(PropertyKey.SKIN_UPLOADED)) {
                return Result.handled();
            }

            byte[] responseData = new byte[data.length - 2];
            System.arraycopy(data, 2, responseData, 0, responseData.length);

            JsonObject response;
            try {
                Reader reader = new InputStreamReader(new ByteArrayInputStream(responseData));
                response = GSON.fromJson(reader, JsonObject.class);
            } catch (JsonIOException | JsonSyntaxException throwable) {
                logger.error("Failed to read Skin response", throwable);
                return Result.handled();
            }

            floodgatePlayer.addProperty(PropertyKey.SKIN_UPLOADED, response);
            skinApplier.applySkin(floodgatePlayer, UploadResult.success(response));
        }

        // Players (Geyser) can't send requests nor responses
        if (sourceIdentity == Identity.PLAYER) {
            return Result.kick("Got Skin " + (request ? "request" : "response") + " from Player?");
        }
        return Result.handled();
    }

    @Override
    public Result handleServerCall(byte[] data, UUID targetUuid, String targetUsername) {
        FloodgatePlayer floodgatePlayer = api.getPlayer(targetUuid);
        if (floodgatePlayer == null) {
            return Result.kick("Non-Floodgate player sent a Skin plugin message");
        }

        // non-proxy servers can only handle requests (from proxies)

        if (!floodgatePlayer.isFromProxy()) {
            return Result.kick("Cannot receive Skin request from Player");
        }

        // 1 byte for isRequest and 9 for RawSkin itself
        if (data.length < Base64Utils.getEncodedLength(9 + 1)) {
            return Result.kick("Skin request data has to be at least 10 byte long.");
        }

        boolean request = data[0] == 1;

        if (!request) {
            return Result.kick("Proxy sent a response instead of a request?");
        }

        RawSkin rawSkin = null;
        try {
            rawSkin = RawSkin.decode(data, 1);
        } catch (Exception exception) {
            logger.error("Failed to decode RawSkin", exception);
        }

        // we let it continue since SkinHandler sends the plugin message for us
        skinHandler.handleServerSkinUpload(floodgatePlayer, rawSkin);

        return Result.handled();
    }

    public boolean sendSkinRequest(UUID player, RawSkin skin) {
        byte[] skinRequestData = createSkinRequestData(skin.encode());
        return pluginMessageUtils.sendMessage(player, true, getIdentifier(), skinRequestData);
    }

    public void sendSkinResponse(UUID player, boolean failed, String response) {
        byte[] skinRequestData = createSkinResponseData(failed, response);
        pluginMessageUtils.sendMessage(player, false, getIdentifier(), skinRequestData);
    }

    public byte[] createSkinRequestData(byte[] data) {
        // data format:
        // 0 = is request
        // remaining = request data

        byte[] output = new byte[data.length + 1];

        output[0] = 1;
        System.arraycopy(data, 0, output, 1, data.length);

        return output;
    }

    public byte[] createSkinResponseData(boolean failed, String data) {
        // data format:
        // 0 = is request
        // 1 = has failed
        // remaining = response data

        byte[] rawData = data.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[rawData.length + 2];

        output[0] = 0;
        output[1] = (byte) (failed ? 1 : 0);
        System.arraycopy(rawData, 0, output, 2, rawData.length);

        return output;
    }
}
