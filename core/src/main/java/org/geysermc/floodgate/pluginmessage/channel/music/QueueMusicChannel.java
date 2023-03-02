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

package org.geysermc.floodgate.pluginmessage.channel.music;

import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel;

public class QueueMusicChannel implements PluginMessageChannel {
    @Inject private PluginMessageUtils pluginMessageUtils;

    @Override
    public String getIdentifier() {
        return "floodgate:music_queue";
    }

    @Override
    public Result handleProxyCall(
        byte[] data,
        UUID sourceUuid,
        String sourceUsername,
        Identity sourceIdentity) {

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
    public Result handleServerCall(byte[] data, UUID targetUuid, String targetUsername) {
        return Result.kick("I'm sorry, I'm unable to queue music on the server :(");
    }

    public boolean sendQueueMusic(UUID player, float fadeSeconds, boolean repeatMode, String trackName, float volume) {
        int fadeSecondsBits = Float.floatToIntBits(fadeSeconds);
        int volumeBits = Float.floatToIntBits(volume);
        byte[] trackNameBytes = trackName.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[trackNameBytes.length + 9];

        data[0] = (byte) (fadeSecondsBits >> 24);
        data[1] = (byte) (fadeSecondsBits >> 16);
        data[2] = (byte) (fadeSecondsBits >> 8);
        data[3] = (byte) (fadeSecondsBits);
        data[4] = (byte) (repeatMode ? 1 : 0);
        data[5] = (byte) (volumeBits >> 24);
        data[6] = (byte) (volumeBits >> 16);
        data[7] = (byte) (volumeBits >> 8);
        data[8] = (byte) (volumeBits);
        System.arraycopy(trackNameBytes, 0, data, 9, trackNameBytes.length);

        return pluginMessageUtils.sendMessage(player, getIdentifier(), data);
    }
}
 