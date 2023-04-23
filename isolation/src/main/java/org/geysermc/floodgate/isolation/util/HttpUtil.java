/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.isolation.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.checkerframework.checker.nullness.qual.NonNull;

public class HttpUtil {
    private static final String USER_AGENT = "GeyserMC-isolation";

    public static @NonNull HttpResponse<byte[]> getRawData(String urlString) throws IOException {
        HttpURLConnection connection = request(urlString);

        try (InputStream inputStream = connection.getInputStream()) {
            return new HttpResponse<>(
                    connection.getResponseCode(),
                    StreamUtil.readStream(inputStream)
            );
        } catch (SocketTimeoutException | NullPointerException exception) {
            return new HttpResponse<>(-1, null);
        }
    }

    private static HttpURLConnection request(String urlString) {
        HttpURLConnection connection;

        try {
            URL url = new URL(urlString.replace(" ", "%20")); // Encode spaces correctly
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        try {
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(10_000);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        }

        return connection;
    }

    public static class HttpResponse<T> {
        private final int httpCode;
        private final T response;

        private HttpResponse(int httpCode, T response) {
            this.httpCode = httpCode;
            this.response = response;
        }

        public int httpCode() {
            return httpCode;
        }

        public T response() {
            return response;
        }

        public boolean isCodeOk() {
            return httpCode >= 200 && httpCode < 300;
        }
    }
}
