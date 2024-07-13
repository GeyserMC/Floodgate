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
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageChannel;

public class TransferChannel implements PluginMessageChannel {
    @Inject private PluginMessageUtils pluginMessageUtils;

    @Override
    public String getIdentifier() {
        return "floodgate:transfer";
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
            handleServerCall(data, sourceUuid, sourceUsername);
        }

        return Result.handled();
    }

    @Override
    public Result handleServerCall(byte[] data, UUID playerUuid, String playerUsername) {
        return Result.kick("I'm sorry, I'm unable to transfer a server :(");
    }

    public boolean sendTransfer(UUID player, String address, int port) {
        byte[] addressBytes = address.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[addressBytes.length + 4];

        data[0] = (byte) (port >> 24);
        data[1] = (byte) (port >> 16);
        data[2] = (byte) (port >> 8);
        data[3] = (byte) (port);
        System.arraycopy(addressBytes, 0, data, 4, addressBytes.length);

        return pluginMessageUtils.sendMessage(player, getIdentifier(), data);
    }
}
