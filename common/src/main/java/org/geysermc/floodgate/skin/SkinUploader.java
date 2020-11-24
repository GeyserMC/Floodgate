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

import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.util.HttpUtils;
import org.geysermc.floodgate.util.HttpUtils.HttpPostResponse;
import org.geysermc.floodgate.util.RawSkin;

public final class SkinUploader {
    private static final String UPLOAD_URL = "https://api.mineskin.org/generate/upload";
    private static final int MAX_TRIES = 3;

    private final Executor requestExecutor = Executors.newSingleThreadExecutor();
    private long nextResult = 0;

    public CompletableFuture<UploadResult> uploadSkin(RawSkin rawSkin) {
        return CompletableFuture.supplyAsync(() -> uploadSkinInner(rawSkin, 0), requestExecutor);
    }

    private UploadResult uploadSkinInner(RawSkin rawSkin, int times) {
        if (System.currentTimeMillis() < nextResult) {
            try {
                Thread.sleep(nextResult - System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        SkinModel model = rawSkin.alex ? SkinModel.ALEX : SkinModel.STEVE;

        String url = UPLOAD_URL + getUploadUrlParameters(model);
        BufferedImage image = SkinUtils.toBufferedImage(rawSkin);

        try {
            UploadResult result = parseAndHandleResponse(HttpUtils.post(url, image));
            if (result.httpCode == 429) {
                times += 1;
                if (times >= MAX_TRIES) {
                    return result;
                }
                uploadSkinInner(rawSkin, times);
            }
            return result;
        } catch (RuntimeException exception) {
            return UploadResult.exception(exception);
        }
    }

    private String getUploadUrlParameters(SkinModel model) {
        return "?visibility=1&model=" + model.name;
    }

    private UploadResult parseAndHandleResponse(HttpPostResponse response) {
        int httpCode = response.getHttpCode();
        JsonObject jsonResponse = response.getResponse();

        if (jsonResponse == null) {
            throw new IllegalStateException("Response cannot be null!");
        }

        nextResult = jsonResponse.get("nextRequest").getAsLong();

        if (httpCode >= 200 && httpCode < 300) {
            return UploadResult.success(httpCode, jsonResponse);
        } else {
            return UploadResult.failed(httpCode, jsonResponse.get("error").getAsString());
        }
    }

    public enum SkinModel {
        STEVE, ALEX;

        public static final SkinModel[] VALUES = values();
        private final String name = name().toLowerCase();

        public static SkinModel getByOrdinal(int ordinal) {
            return VALUES.length > ordinal ? VALUES[ordinal] : STEVE;
        }

        public static SkinModel getByName(String name) {
            return name == null || !name.equalsIgnoreCase("alex") ? STEVE : ALEX;
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class UploadResult {
        private final int httpCode;
        private final String error;
        private final boolean exception;

        private final SkinModel model;
        private final String skinUrl;
        private final String capeUrl;
        private final JsonObject response;

        public static UploadResult exception(Throwable throwable) {
            return new UploadResult(-1, throwable.getMessage(), true, null, null, null, null);
        }

        public static UploadResult failed(int httpCode, String error) {
            return new UploadResult(httpCode, error, false, SkinModel.STEVE, null, null, null);
        }

        public static UploadResult success(int httpCode, JsonObject body) {
            SkinModel model = SkinModel.getByName(body.get("model").getAsString());

            JsonObject data = body.getAsJsonObject("data");
            JsonObject textureData = data.getAsJsonObject("texture");

            JsonObject urls = textureData.getAsJsonObject("urls");
            String skinUrl = urls.get("skin").getAsString();
            String capeUrl = urls.has("cape") ? urls.get("cape").getAsString() : null;

            JsonObject response = new JsonObject();
            response.addProperty("value", textureData.get("value").getAsString());
            response.addProperty("signature", textureData.get("signature").getAsString());

            return new UploadResult(httpCode, null, false, model, skinUrl, capeUrl, response);
        }
    }
}
