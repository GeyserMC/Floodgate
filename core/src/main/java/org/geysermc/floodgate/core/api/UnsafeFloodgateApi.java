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

package org.geysermc.floodgate.core.api;

import java.util.UUID;
import org.geysermc.floodgate.api.unsafe.Unsafe;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.core.pluginmessage.channel.PacketChannel;

public final class UnsafeFloodgateApi implements Unsafe {
    private final PacketChannel packetChannel;

    UnsafeFloodgateApi(PluginMessageManager pluginMessageManager) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        if (!SimpleFloodgateApi.class.getName().equals(element.getClassName())) {
            throw new IllegalStateException("Use the Floodgate api to get an instance");
        }

        packetChannel = pluginMessageManager.getChannel(PacketChannel.class);
    }

    @Override
    public void sendPacket(UUID bedrockPlayer, int packetId, byte[] packetData) {
        byte[] fullData = new byte[packetData.length + 1];
        fullData[0] = (byte) packetId;
        System.arraycopy(packetData, 0, fullData, 1, packetData.length);

        packetChannel.sendPacket(bedrockPlayer, fullData, this);
    }
}
