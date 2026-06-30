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

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpClient;
import org.geysermc.floodgate.util.MojangUtils;

@Singleton
final class MinecraftServerSkinFallback {
    @Inject private FloodgateApi api;
    @Inject private SkinApplier applier;
    @Inject private HttpClient httpClient;
    @Inject private MojangUtils mojangUtils;
    @Inject private FloodgateLogger logger;

    int applyForSubscription(int subscribeId, String verifyCode, String trigger,
            boolean skinUploadDebug) {
        int matchedPlayers = 0;

        for (FloodgatePlayer player : api.getPlayers()) {
            if (!(player instanceof FloodgatePlayerImpl)) {
                continue;
            }

            FloodgatePlayerImpl floodgatePlayer = (FloodgatePlayerImpl) player;
            if (floodgatePlayer.getSubscribeId() != subscribeId ||
                    !Objects.equals(floodgatePlayer.getVerifyCode(), verifyCode)) {
                continue;
            }

            matchedPlayers++;
            fetchAndApplyMinecraftServerSkin(floodgatePlayer, trigger, skinUploadDebug);
        }

        return matchedPlayers;
    }

    private void fetchAndApplyMinecraftServerSkin(FloodgatePlayer player, String trigger,
            boolean skinUploadDebug) {
        if (!player.isLinked()) {
            fetchAndApplyBedrockSkinFromGlobalApi(player, trigger, skinUploadDebug);
            return;
        }

        fetchAndApplyJavaSkinFromMojang(player, trigger, skinUploadDebug);
        }

        private void fetchAndApplyBedrockSkinFromGlobalApi(FloodgatePlayer player, String trigger,
            boolean skinUploadDebug) {
        String xuid = player.getXuid();
        if (xuid == null || xuid.isEmpty()) {
            logger.warn("Bedrock fallback couldn't run for {} (trigger={}) because xuid was missing",
                player.getCorrectUsername(), trigger);
            return;
        }

        logSkinDebug(skinUploadDebug,
            "Triggering Bedrock fallback via global API for player={} xuid={} trigger={}",
            player.getCorrectUsername(), xuid, trigger);

        httpClient.asyncGet(Constants.GET_BEDROCK_SKIN_URL + xuid, JsonObject.class)
            .whenComplete((response, throwable) -> {
                if (throwable != null) {
                logger.warn("Global API Bedrock skin fallback failed for {} (trigger={}): {}",
                    player.getCorrectUsername(), trigger, throwable.getMessage());
                return;
                }

                if (response == null || !response.isCodeOk()) {
                int httpCode = response != null ? response.getHttpCode() : -1;
                logger.warn("Global API Bedrock skin fallback returned HTTP {} for {} (trigger={})",
                    httpCode, player.getCorrectUsername(), trigger);
                return;
                }

                SkinData resolvedSkin = skinDataFromGlobalApiResponse(response.getResponse());
                if (resolvedSkin == null || isDefaultSkin(resolvedSkin)) {
                logger.warn("Global API Bedrock skin fallback returned no usable skin for {} (trigger={})",
                    player.getCorrectUsername(), trigger);
                return;
                }

                applyResolvedSkin(player, resolvedSkin, trigger, skinUploadDebug,
                    "global-api-bedrock");
            });
        }

        private void fetchAndApplyJavaSkinFromMojang(FloodgatePlayer player, String trigger,
            boolean skinUploadDebug) {
        logSkinDebug(skinUploadDebug,
            "Triggering Java fallback via Mojang session server for player={} trigger={}",
                player.getCorrectUsername(), trigger);

        mojangUtils.skinFor(player.getCorrectUniqueId()).whenComplete((skinData, throwable) -> {
            if (throwable != null) {
            logger.warn("Mojang session-server fallback failed for {} (trigger={}): {}",
                        player.getCorrectUsername(), trigger, throwable.getMessage());
                return;
            }

            SkinData resolvedSkin = skinData != null ? skinData : SkinDataImpl.DEFAULT_SKIN;
            if (isDefaultSkin(resolvedSkin)) {
                if (!player.isLinked()) {
                    logger.warn("Minecraft session-server fallback returned default skin for unlinked Bedrock player {} (trigger={}). " +
                                    "Mojang session servers do not provide Bedrock skins; keeping current skin.",
                            player.getCorrectUsername(), trigger);
                } else {
                    logger.warn("Mojang session-server fallback returned default skin for {} (trigger={}). " +
                                    "Keeping current skin to avoid overriding with Steve/Alex.",
                            player.getCorrectUsername(), trigger);
                }
                return;
            }

            applyResolvedSkin(player, resolvedSkin, trigger, skinUploadDebug,
                    "mojang-sessionserver");
        });
    }

    private SkinData skinDataFromGlobalApiResponse(JsonObject response) {
        if (response == null || !response.has("value") || !response.has("signature")) {
            return null;
        }

        try {
            String value = response.get("value").getAsString();
            String signature = response.get("signature").getAsString();
            if (value == null || value.isEmpty() || signature == null || signature.isEmpty()) {
                return null;
            }
            return new SkinDataImpl(value, signature);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyResolvedSkin(FloodgatePlayer player, SkinData resolvedSkin, String trigger,
            boolean skinUploadDebug, String source) {
            applier.applySkin(player, resolvedSkin, true);
            if (!player.isLinked()) {
                player.addProperty(PropertyKey.SKIN_UPLOADED, resolvedSkin);
            }

            logSkinDebug(skinUploadDebug,
                    "Fallback skin applied for player={} trigger={} source={}",
                    player.getCorrectUsername(), trigger, source);
    }

    private void logSkinDebug(boolean enabled, String message, Object... args) {
        if (enabled) {
            logger.info("[skin-upload-debug] " + message, args);
        }
    }

    private boolean isDefaultSkin(SkinData skinData) {
        return skinData == null ||
                (Constants.DEFAULT_MINECRAFT_JAVA_SKIN_TEXTURE.equals(skinData.value()) &&
                        Constants.DEFAULT_MINECRAFT_JAVA_SKIN_SIGNATURE.equals(skinData.signature()));
    }
}