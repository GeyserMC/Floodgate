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

package org.geysermc.floodgate.core.pluginmessage.channel;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.core.skin.SkinDataImpl;

public class SkinChannel implements PluginMessageChannel {
    @Inject GeyserApiBase api;
    @Inject FloodgateConfig config;
    @Inject SkinApplier skinApplier;

    @Override
    public String getIdentifier() {
        return "floodgate:skin";
    }

    @Override
    public Result handleProxyCall(
            byte[] data,
            UUID sourceUuid,
            String sourceUsername,
            Identity sourceIdentity
    ) {
        // we can only get skins from Geyser (client)
        if (sourceIdentity == Identity.PLAYER) {
            Result result = handleServerCall(data, sourceUuid, sourceUsername);
            // aka translate 'handled' into 'forward' when send-floodgate-data is enabled
            if (!result.isAllowed() && result.getReason() == null) {
                if (config.proxy() && ((ProxyFloodgateConfig) config).sendFloodgateData()) {
                    return Result.forward();
                }
            }
            return result;
        }

        // Servers can't send skin data
        if (sourceIdentity == Identity.SERVER) {
            return Result.kick("Got skin data from a server?");
        }
        return Result.handled();
    }

    @Override
    public Result handleServerCall(byte[] data, UUID playerUuid, String playerUsername) {
        Connection connection = api.connectionByUuid(playerUuid);
        if (connection == null) {
            return Result.kick("Player sent skins data for a non-Floodgate player");
        }

        String message = new String(data, StandardCharsets.UTF_8);

        String[] split = message.split("\0");
        // value and signature
        if (split.length != 2) {
            return Result.kick("Got invalid skin data");
        }

        String value = split[0];
        String signature = split[1];

        SkinData skinData = new SkinDataImpl(value, signature);

        skinApplier.applySkin(connection, skinData);

        return Result.handled();
    }
}
