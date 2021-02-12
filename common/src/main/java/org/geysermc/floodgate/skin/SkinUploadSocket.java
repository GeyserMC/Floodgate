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

import static org.geysermc.floodgate.util.Constants.WEBSOCKET_URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import lombok.Getter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.Utils;
import org.geysermc.floodgate.util.WebsocketEventType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

final class SkinUploadSocket extends WebSocketClient {
    private static final Gson gson = new Gson();

    private final SkinUploadManager uploadManager;
    private final FloodgateApi api;
    private final SkinApplier applier;
    private final FloodgateLogger logger;

    @Getter private final int id;
    @Getter private final String verifyCode;
    @Getter private int subscribersCount;

    public SkinUploadSocket(
            int id,
            String verifyCode,
            SkinUploadManager uploadManager,
            FloodgateApi api,
            SkinApplier applier,
            FloodgateLogger logger) {

        super(getWebsocketUri(id, verifyCode));
        this.id = id;
        this.verifyCode = verifyCode;
        this.uploadManager = uploadManager;
        this.api = api;
        this.applier = applier;
        this.logger = logger;
    }

    private static URI getWebsocketUri(int id, String verifyCode) {
        try {
            return new URI(WEBSOCKET_URL + "?subscribed_to=" + id + "&verify_code=" + verifyCode);
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error while creating uri. Id = " + id + ", verify_code = " + verifyCode,
                    exception);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        setConnectionLostTimeout(11);
    }

    @Override
    public void onMessage(String data) {
        JsonObject message = gson.fromJson(data, JsonObject.class);
        if (message.has("error")) {
            logger.error("Skin uploader got an error: {}", message.get("error").getAsString());
        }

        int typeId = message.get("event_id").getAsInt();
        WebsocketEventType type = WebsocketEventType.getById(typeId);
        if (type == null) {
            logger.warn("Got unknown type {}. Ensure that Floodgate is up-to-date", typeId);
            return;
        }

        if (type == WebsocketEventType.SUBSCRIBERS_COUNT) {
            subscribersCount = message.get("subscribers_count").getAsInt();
        } else if (type == WebsocketEventType.SKIN_UPLOADED) {
            String xuid = message.get("xuid").getAsString();
            FloodgatePlayer player = api.getPlayer(Utils.getJavaUuid(xuid));
            if (player != null) {
                applier.applySkin(player, message);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (reason != null && !reason.isEmpty()) {
            JsonObject message = gson.fromJson(reason, JsonObject.class);

            // info means that the uploader itself did nothing wrong
            if (message.has("info")) {
                String info = message.get("info").getAsString();
                logger.debug("Got disconnected from the skin uploader: {}", info);
            }

            // error means that the uploader did something wrong
            if (message.has("error")) {
                String error = message.get("error").getAsString();
                logger.info("Got disconnected from the skin uploader: {}", error);
            }
        }

        uploadManager.removeConnection(id, this);
    }

    @Override
    public void onError(Exception exception) {
        logger.error("Got an error", exception);
    }
}
