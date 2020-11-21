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

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;
import org.geysermc.floodgate.util.RawSkin;

public final class SkinUploader {
    private static final String UPLOAD_URL = "https://api.mineskin.org/generate/upload";
    private static final String USER_AGENT = "GeyserMC/Floodgate";
    private static final MediaType PNG_TYPE = MediaType.get("image/png");
    private static final Gson GSON = new Gson();

    private final Executor requestExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient =
            new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

    private long nextResult = 0;

    public CompletableFuture<UploadResult> uploadSkin(RawSkin rawSkin) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (System.currentTimeMillis() < nextResult) {
                        try {
                            Thread.sleep(nextResult - System.currentTimeMillis());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    SkinModel model = rawSkin.alex ? SkinModel.ALEX : SkinModel.STEVE;

                    BufferedImage image = SkinUtils.toBufferedImage(rawSkin);
                    // a black 32x32 png was 133 bytes, so we assume that it's at least 133 bytes
                    ByteArrayOutputStream stream = new ByteArrayOutputStream(133);
                    try {
                        ImageIO.write(image, "png", stream);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    RequestBody fileData = RequestBody.create(PNG_TYPE, stream.toByteArray());

                    RequestBody body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "skin.png", fileData)
                            .build();

                    System.out.println("final file stream size: " + stream.size());

                    Buffer buffer = new Buffer();
                    try {
                        body.writeTo(buffer);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    Request request = new Request.Builder()
                            .url(UPLOAD_URL + getUploadUrlParameters(model))
                            .header(HttpHeaders.USER_AGENT, USER_AGENT)
                            .post(body)
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.body() == null) {
                            throw new RuntimeException("Response didn't have a body!");
                        }
                        return parseAndHandleResponse(response.code(), response.body().source());
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                },
                requestExecutor
        );
    }

    private String getUploadUrlParameters(SkinModel model) {
        return "?visibility=1&model=" + model.name;
    }

    private UploadResult parseAndHandleResponse(int httpCode, BufferedSource response) {
        InputStreamReader reader = new InputStreamReader(response.inputStream());
        JsonObject jsonResponse = GSON.fromJson(reader, JsonObject.class);

        if (jsonResponse == null) {
            throw new IllegalStateException("Response cannot be null!");
        }

        System.out.println(jsonResponse.toString());

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

        private final SkinModel model;
        private final String skinUrl;
        private final String capeUrl;
        private final JsonObject response;

        public static UploadResult failed(int httpCode, String error) {
            return new UploadResult(httpCode, error, SkinModel.STEVE, null, null, null);
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

            return new UploadResult(httpCode, null, model, skinUrl, capeUrl, response);
        }
    }
}
