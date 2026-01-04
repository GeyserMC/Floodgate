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

package org.geysermc.floodgate.skin;

import static org.geysermc.floodgate.util.Constants.WEBSOCKET_URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.net.ConnectException;
import java.net.URI;
import javax.net.ssl.SSLException;
import lombok.Getter;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
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
            FloodgateLogger logger
    ) {
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
    public void onOpen(ServerHandshake ignored) {
        setConnectionLostTimeout(11);
    }

    @Override
    public void onMessage(String data) {
        JsonObject message = gson.fromJson(data, JsonObject.class);
        if (message.has("error")) {
            logger.error("Skin uploader got an error: {}", message.get("error").getAsString());
        }

        int typeId = message.get("event_id").getAsInt();
        WebsocketEventType type = WebsocketEventType.fromId(typeId);
        if (type == null) {
            logger.warn("Got unknown type {}. Ensure that Floodgate is up-to-date", typeId);
            return;
        }

        switch (type) {
            case SUBSCRIBER_COUNT:
                subscribersCount = message.get("subscribers_count").getAsInt();
                break;
            case SKIN_UPLOADED:
                String xuid = message.get("xuid").getAsString();
                FloodgatePlayer player = api.getPlayer(Utils.getJavaUuid(xuid));
                if (player != null) {
                    if (!message.get("success").getAsBoolean()) {
                        logger.info("Failed to upload skin for {} ({})", xuid,
                                player.getCorrectUsername());
                        return;
                    }

                    SkinData skinData = SkinDataImpl.from(message.getAsJsonObject("data"));
                    applier.applySkin(player, skinData, false);

                    // legacy stuff,
                    // will be removed shortly after or during the Floodgate-Geyser integration
                    if (!player.isLinked()) {
                        player.addProperty(PropertyKey.SKIN_UPLOADED, skinData);
                    }
                }
                break;
            case LOG_MESSAGE:
                String logMessage = message.get("message").getAsString();
                switch (message.get("priority").getAsInt()) {
                    case -1:
                        logger.debug("Got a message from skin uploader: " + logMessage);
                        break;
                    case 0:
                        logger.info("Got a message from skin uploader: " + logMessage);
                        break;
                    case 1:
                        logger.error("Got a message from skin uploader: " + logMessage);
                        break;
                    default:
                        logger.info(logMessage);
                        break;
                }
                break;
            default:
                // we don't handle the remaining types
                break;
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
        // skip can't connect exceptions and the syntax error in onClose that happens because of it.
        // they might however help during debugging so we'll log them when debug is enabled
        if (exception instanceof ConnectException || exception instanceof JsonSyntaxException ||
                exception instanceof SSLException) {
            if (logger.isDebug()) {
                logger.error("[debug] Got an error", exception);
            }
            return;
        }
        logger.error("Got an error", exception);
    }
}
