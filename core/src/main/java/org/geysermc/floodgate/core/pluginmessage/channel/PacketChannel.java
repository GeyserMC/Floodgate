/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import com.google.inject.Inject;
import java.util.UUID;
import org.geysermc.floodgate.core.api.UnsafeFloodgateApi;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;

public final class PacketChannel implements PluginMessageChannel {
    @Inject private PluginMessageUtils pluginMessageUtils;

    @Override
    public String getIdentifier() {
        return "floodgate:packet";
    }

    @Override
    public Result handleProxyCall(
            byte[] data,
            UUID sourceUuid,
            String sourceUsername,
            Identity sourceIdentity
    ) {
        if (sourceIdentity == Identity.SERVER) {
            // send it to the client
            return Result.forward();
        }

        if (sourceIdentity == Identity.PLAYER) {
            return handleServerCall(data, sourceUuid, sourceUsername);
        }

        return Result.handled();
    }

    @Override
    public Result handleServerCall(byte[] data, UUID playerUuid, String playerUsername) {
        return Result.kick("Cannot send packets from Geyser/Floodgate to Floodgate");
    }

    public boolean sendPacket(UUID player, byte[] packet, UnsafeFloodgateApi api) {
        if (api == null) {
            throw new IllegalArgumentException("Can only send a packet using the unsafe api");
        }
        return pluginMessageUtils.sendMessage(player, getIdentifier(), packet);
    }
}
