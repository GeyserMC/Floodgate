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

package org.geysermc.floodgate.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class WebEndpoints {
    @Inject
    private HttpClient httpClient;

    public CompletableFuture<Long> getXuidFor(String gamertag) {
        if (gamertag == null || gamertag.isEmpty() || gamertag.length() > 16) {
            return Utils.failedFuture(new IllegalStateException("Received an invalid gamertag"));
        }

        return httpClient.asyncGet(Constants.GET_XUID_URL + gamertag)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();

                    if (!result.isCodeOk()) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonElement xuid = response.get("xuid");
                    return xuid != null ? xuid.getAsLong() : null;
                });
    }

    public CompletableFuture<String> getGamertagFor(long xuid) {
        return httpClient.asyncGet(Constants.GET_GAMERTAG_URL + xuid)
                .thenApply(result -> {
                    JsonObject response = result.getResponse();

                    if (!result.isCodeOk()) {
                        throw new IllegalStateException(response.get("message").getAsString());
                    }

                    JsonElement gamertag = response.get("gamertag");
                    return gamertag != null ? gamertag.getAsString() : null;
                });
    }
}
