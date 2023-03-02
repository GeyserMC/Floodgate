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
import java.util.UUID;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel;

public class StopMusicChannel implements PluginMessageChannel {
    @Inject private PluginMessageUtils pluginMessageUtils;

    @Override
    public String getIdentifier() {
        return "floodgate:music_stop";
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
        return Result.kick("I'm sorry, I'm unable to stop music on the server :(");
    }

    public boolean sendStopMusic(UUID player, float fadeSeconds) {
        int fadeSecondsBits = Float.floatToIntBits(fadeSeconds);
        byte[] data = new byte[4];

        data[0] = (byte) (fadeSecondsBits >> 24);
        data[1] = (byte) (fadeSecondsBits >> 16);
        data[2] = (byte) (fadeSecondsBits >> 8);
        data[3] = (byte) (fadeSecondsBits);

        return pluginMessageUtils.sendMessage(player, getIdentifier(), data);
    }
}