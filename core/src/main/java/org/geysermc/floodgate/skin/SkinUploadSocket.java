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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
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
    private static final int SKIN_UPLOAD_EVENT_WAIT_SECONDS = 15;
    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile(
            "(?i)status code received:\\s*(\\d{3})|HTTP/\\d\\.\\d\\s+(\\d{3})");

    private final SkinUploadManager uploadManager;
    private final FloodgateApi api;
    private final SkinApplier applier;
    private final MinecraftServerSkinFallback minecraftServerSkinFallback;
    private final FloodgateLogger logger;
    private final boolean skinUploadDebug;

    @Getter private final int id;
    @Getter private final String verifyCode;
    @Getter private int subscribersCount;
    private volatile boolean receivedSkinUploadedEvent;
    private volatile boolean shouldReconnect = true;
    private volatile boolean fallbackTriggered;

    public SkinUploadSocket(
            int id,
            String verifyCode,
            SkinUploadManager uploadManager,
            FloodgateApi api,
            SkinApplier applier,
            MinecraftServerSkinFallback minecraftServerSkinFallback,
            FloodgateLogger logger,
            boolean skinUploadDebug
    ) {
        super(getWebsocketUri(id, verifyCode));
        this.id = id;
        this.verifyCode = verifyCode;
        this.uploadManager = uploadManager;
        this.api = api;
        this.applier = applier;
        this.minecraftServerSkinFallback = minecraftServerSkinFallback;
        this.logger = logger;
        this.skinUploadDebug = skinUploadDebug;
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
        receivedSkinUploadedEvent = false;
        shouldReconnect = true;
        fallbackTriggered = false;
        logSkinDebug("WebSocket connected for subscribed_to={} endpoint={}", id,
                sanitizedEndpoint());
        scheduleMissingSkinUploadWarning();
    }

    @Override
    public void onMessage(String data) {
        logSkinDebug("Incoming websocket payload for subscribed_to={}: {}", id, data);

        JsonObject message = gson.fromJson(data, JsonObject.class);
        if (message.has("error")) {
            logger.error("Skin uploader got an error: {}", message.get("error").getAsString());
        }

        int typeId = message.get("event_id").getAsInt();
        WebsocketEventType type = WebsocketEventType.fromId(typeId);
        if (type == null) {
            logger.warn("Got unknown type {}. Ensure that Floodgate is up-to-date", typeId);
            logSkinDebug("Unknown event type {} for payload: {}", typeId, data);
            return;
        }

        logSkinDebug("Received websocket event {} for subscribed_to={}", type, id);

        switch (type) {
            case SUBSCRIBER_COUNT:
                subscribersCount = message.get("subscribers_count").getAsInt();
                logSkinDebug("Subscriber count updated to {} for subscribed_to={}",
                        subscribersCount, id);
                break;
            case SKIN_UPLOADED:
                String xuid = message.get("xuid").getAsString();
                if (!message.get("success").getAsBoolean()) {
                    FloodgatePlayer player = api.getPlayer(Utils.getJavaUuid(xuid));
                    if (player != null) {
                        logger.info("Failed to upload skin for {} ({})", xuid,
                                player.getCorrectUsername());
                    } else {
                        logger.info("Failed to upload skin for {}", xuid);
                    }
                    logSkinDebug("Skin upload failure payload for xuid {}: {}", xuid, data);
                    triggerMinecraftFallback("uploader-reported-failed-upload");
                    // Mark the upload response as received after triggering fallback,
                    // so timeout-based fallback doesn't log a misleading warning.
                    receivedSkinUploadedEvent = true;
                    return;
                }

                receivedSkinUploadedEvent = true;

                SkinData skinData = SkinDataImpl.from(message.getAsJsonObject("data"));
                logSkinDebug("SKIN_UPLOADED success for xuid {} valueLength={} signatureLength={}",
                    xuid, skinData.value().length(), skinData.signature().length());
                logSkinDebug("SKIN_UPLOADED decoded value for xuid {}: {}", xuid,
                    decodeSkinValuePlainText(skinData.value()));
                applySkinOrRetry(xuid, skinData, true);
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
                logSkinDebug("No direct handler for websocket event {}. Raw payload: {}", type,
                        data);
                break;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        boolean serverSideHttpError = false;

        if (reason == null || reason.isEmpty()) {
            logSkinDebug("WebSocket closed without reason (code={}, remote={})", code, remote);
        }

        if (reason != null && !reason.isEmpty()) {
            serverSideHttpError = logCloseReason(code, reason, remote);
        }

        if (serverSideHttpError) {
            triggerMinecraftFallback("uploader-http-5xx");
        }

        uploadManager.removeConnection(id, this, shouldReconnect);
    }

    private boolean logCloseReason(int code, String reason, boolean remote) {
        logSkinDebug("WebSocket close reason payload (code={}, remote={}): {}", code, remote,
                reason);
        boolean serverSideHttpError = warnIfUploaderServerIsDown(reason);
        try {
            JsonElement element = gson.fromJson(reason, JsonElement.class);

            switch (getCloseReasonType(element)) {
                case OBJECT:
                    logJsonCloseReason(code, reason, remote, element.getAsJsonObject());
                    break;
                case PRIMITIVE:
                    logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with primitive reason: {}",
                            code, remote, reason);
                    break;
                case ARRAY:
                    logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with array reason: {}",
                            code, remote, reason);
                    break;
                case NULL:
                    logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with null reason",
                            code, remote);
                    break;
                default:
                    logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with non-object reason: {}",
                            code, remote, reason);
                    break;
            }
        } catch (JsonSyntaxException ignored) {
            logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with raw reason: {}",
                    code, remote, reason);
        }

        return serverSideHttpError;
    }

    private CloseReasonType getCloseReasonType(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return CloseReasonType.NULL;
        }
        if (element.isJsonObject()) {
            return CloseReasonType.OBJECT;
        }
        if (element.isJsonPrimitive()) {
            return CloseReasonType.PRIMITIVE;
        }
        if (element.isJsonArray()) {
            return CloseReasonType.ARRAY;
        }
        return CloseReasonType.UNKNOWN;
    }

    private void logJsonCloseReason(int code, String reason, boolean remote, JsonObject message) {
        boolean hasInfo = message.has("info");
        boolean hasError = message.has("error");

        // info means that the uploader itself did nothing wrong
        if (hasInfo) {
            String info = message.get("info").getAsString();
            updateReconnectPolicy(info, null);
            logger.debug("Got disconnected from the skin uploader (code={}, remote={}): {}",
                    code, remote, info);
        }

        // error means that the uploader did something wrong
        if (hasError) {
            String error = message.get("error").getAsString();
            updateReconnectPolicy(null, error);
            logger.info("Got disconnected from the skin uploader (code={}, remote={}): {}",
                    code, remote, error);
        }

        if (!hasInfo && !hasError) {
            logger.debug("Got disconnected from the skin uploader (code={}, remote={}) with JSON reason: {}",
                    code, remote, reason);
        }
    }

    private void applySkinOrRetry(String xuid, SkinData skinData, boolean firstTry) {
        FloodgatePlayer player = api.getPlayer(Utils.getJavaUuid(xuid));
        if (player == null) {
            if (firstTry) {
                // Skin uploads can arrive before the player is added to the API map.
                logSkinDebug("Player for xuid {} not available yet; scheduling retry", xuid);
                uploadManager.scheduleRetry(() -> applySkinOrRetry(xuid, skinData, false),
                        1, TimeUnit.SECONDS);
            } else {
                logSkinDebug("Player for xuid {} still missing after retry; skipping skin apply",
                        xuid);
            }
            return;
        }

        logSkinDebug("Applying skin for xuid {} (player={})", xuid, player.getCorrectUsername());
        applier.applySkin(player, skinData, false);
        logSkinDebug("Skin apply invocation completed for xuid {} (player={})", xuid,
            player.getCorrectUsername());

        // legacy stuff,
        // will be removed shortly after or during the Floodgate-Geyser integration
        if (!player.isLinked()) {
            player.addProperty(PropertyKey.SKIN_UPLOADED, skinData);
        }
    }

    @Override
    public void onError(Exception exception) {
        // skip can't connect exceptions and the syntax error in onClose that happens because of it.
        // they might however help during debugging so we'll log them when debug is enabled
        if (exception instanceof ConnectException || exception instanceof JsonSyntaxException ||
                exception instanceof SSLException) {
            if (skinUploadDebug || logger.isDebug()) {
                logger.error("[skin-upload-debug] WebSocket transient error", exception);
            }
            if (logger.isDebug()) {
                logger.error("[debug] Got an error", exception);
            }
            return;
        }

        if (skinUploadDebug) {
            logger.error("[skin-upload-debug] WebSocket fatal error", exception);
        }
        logger.error("Got an error", exception);
    }

    private void logSkinDebug(String message, Object... args) {
        if (skinUploadDebug) {
            logger.info("[skin-upload-debug] " + message, args);
        }
    }

    private void scheduleMissingSkinUploadWarning() {
        uploadManager.scheduleRetry(() -> {
            if (receivedSkinUploadedEvent || !isOpen()) {
                return;
            }

            if (skinUploadDebug) {
                logger.info("[skin-upload-debug] No SKIN_UPLOADED event received after {}s for subscribed_to={} (pending subscribers_count={}). " +
                                "Trying Minecraft session server fallback.",
                        SKIN_UPLOAD_EVENT_WAIT_SECONDS, id, subscribersCount);
            }

            triggerMinecraftFallback("missing-skin-uploaded-event-timeout");
        }, SKIN_UPLOAD_EVENT_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private String sanitizedEndpoint() {
        URI uri = getURI();
        if (uri == null) {
            return "<unknown>";
        }

        StringBuilder endpoint = new StringBuilder();
        if (uri.getScheme() != null) {
            endpoint.append(uri.getScheme()).append("://");
        }
        if (uri.getHost() != null) {
            endpoint.append(uri.getHost());
        }
        if (uri.getPort() > -1) {
            endpoint.append(':').append(uri.getPort());
        }
        if (uri.getPath() != null) {
            endpoint.append(uri.getPath());
        }
        endpoint.append("?subscribed_to=").append(id).append("&verify_code=<hidden>");
        return endpoint.toString();
    }

    private String decodeSkinValuePlainText(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "<invalid base64: " + exception.getMessage() + ">";
        }
    }

    private void updateReconnectPolicy(String info, String error) {
        if (info != null && "creator left and there are no uploads left".equalsIgnoreCase(info)) {
            disableReconnect("creator-disconnected-no-uploads");
        }

        if (error != null) {
            String normalized = error.toLowerCase(Locale.ROOT);
            if (normalized.contains("failed to find the given code in combination with the verify code")) {
                disableReconnect("invalid-subscribe-or-verify-code");
            }
        }
    }

    private void disableReconnect(String reason) {
        if (!shouldReconnect) {
            return;
        }

        shouldReconnect = false;
        logSkinDebug("Disabling reconnect for subscribed_to={} due to terminal reason={}", id,
                reason);
    }

    private boolean warnIfUploaderServerIsDown(String reason) {
        Integer statusCode = extractHttpStatusCode(reason);
        if (statusCode == null || statusCode < 500 || statusCode >= 600) {
            return false;
        }

        logger.warn("Skin uploader returned HTTP {}. The upstream skin server may be down or unstable. Reason: {}",
                statusCode, reason);
        return true;
    }

    private Integer extractHttpStatusCode(String reason) {
        if (reason == null || reason.isEmpty()) {
            return null;
        }

        Matcher matcher = HTTP_STATUS_PATTERN.matcher(reason);
        if (!matcher.find()) {
            return null;
        }

        String first = matcher.group(1);
        String second = matcher.group(2);
        String value = first != null ? first : second;
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void triggerMinecraftFallback(String trigger) {
        triggerMinecraftFallback(trigger, true);
    }

    private void triggerMinecraftFallback(String trigger, boolean allowRetry) {
        if (fallbackTriggered || receivedSkinUploadedEvent) {
            return;
        }

        fallbackTriggered = true;
        int matchedPlayers = minecraftServerSkinFallback.applyForSubscription(
                id, verifyCode, trigger, skinUploadDebug);

        if (matchedPlayers == 0) {
            if (allowRetry) {
                fallbackTriggered = false;
                uploadManager.scheduleRetry(() -> triggerMinecraftFallback(trigger, false),
                        1, TimeUnit.SECONDS);
                return;
            }

            logger.warn("Could not run Minecraft session-server fallback for subscribed_to={} (trigger={}) because no matching player was found",
                    id, trigger);
        }
    }

    private enum CloseReasonType {
        OBJECT,
        PRIMITIVE,
        ARRAY,
        NULL,
        UNKNOWN
    }
}
