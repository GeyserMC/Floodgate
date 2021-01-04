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

package org.geysermc.floodgate.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("all")
public class HttpUtils {
    private static final Gson GSON = new Gson();
    private static final String USER_AGENT = "GeyserMC/Floodgate";
    private static final String CONNECTION_STRING = "--";
    private static final String BOUNDARY = "******";
    private static final String END = "\r\n";

    public static HttpResponse get(String urlString) {
        HttpURLConnection connection;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        try {
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("ContentType", "application/json");
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        }

        return readResponse(connection);
    }

    public static HttpResponse post(String urlString, BufferedImage... images) {
        HttpURLConnection connection;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        DataOutputStream outputStream = null;

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data;boundary=" + BOUNDARY
            );

            outputStream = new DataOutputStream(connection.getOutputStream());
            writeDataFor(outputStream, images);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        } finally {
            try {
                outputStream.close();
            } catch (Exception ignored) {
            }
        }

        return readResponse(connection);
    }

    private static HttpResponse readResponse(HttpURLConnection connection) {
        InputStream stream = null;
        try {
            stream = connection.getInputStream();
        } catch (Exception exception) {
            try {
                stream = connection.getErrorStream();
            } catch (Exception exception1) {
                throw new RuntimeException("Both the input and the error stream failed?!");
            }
        }

        InputStreamReader streamReader = new InputStreamReader(stream);

        try {
            int responseCode = connection.getResponseCode();

            JsonObject response = GSON.fromJson(streamReader, JsonObject.class);

            return new HttpResponse(responseCode, response);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read response", exception);
        } finally {
            try {
                streamReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static void writeDataFor(DataOutputStream outputStream, BufferedImage... images) {
        try {
            for (int i = 0; i < images.length; i++) {
                outputStream.writeBytes(CONNECTION_STRING + BOUNDARY + END);
                outputStream.writeBytes(
                        "Content-Disposition:form-data;name=file;filename=image" + i + ".png");
                outputStream.writeBytes(END);
                outputStream.writeBytes(END);
                fileDataForImage(outputStream, images[i]);
                outputStream.writeBytes(END);
            }
            outputStream.writeBytes(CONNECTION_STRING + BOUNDARY + CONNECTION_STRING + END);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void fileDataForImage(OutputStream outputStream, BufferedImage image) {
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class HttpResponse {
        private final int httpCode;
        private final JsonObject response;
    }
}
