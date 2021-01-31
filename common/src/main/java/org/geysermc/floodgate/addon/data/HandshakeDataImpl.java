/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.addon.data;

import io.netty.channel.Channel;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.geysermc.floodgate.util.RawSkin;
import org.geysermc.floodgate.util.Utils;

@Getter
public class HandshakeDataImpl implements HandshakeData {
    private final Channel channel;
    private final boolean floodgatePlayer;
    private final BedrockData bedrockData;
    private final String javaUsername;
    private final UUID javaUniqueId;

    @Setter private LinkedPlayer linkedPlayer;
    @Setter private RawSkin rawSkin;
    @Setter private String hostname;
    @Setter private String bedrockIp;
    @Setter private String disconnectReason;

    public HandshakeDataImpl(
            Channel channel,
            boolean floodgatePlayer,
            BedrockData bedrockData,
            FloodgateConfig config,
            LinkedPlayer linkedPlayer,
            RawSkin rawSkin,
            String hostname) {

        this.channel = channel;
        this.floodgatePlayer = floodgatePlayer;
        this.bedrockData = bedrockData;
        this.linkedPlayer = linkedPlayer;
        this.rawSkin = rawSkin;
        this.hostname = hostname;

        String javaUsername = null;
        UUID javaUniqueId = null;

        if (bedrockData != null) {
            String prefix = config.getUsernamePrefix();
            int usernameLength = Math.min(bedrockData.getUsername().length(), 16 - prefix.length());
            javaUsername = prefix + bedrockData.getUsername().substring(0, usernameLength);
            if (config.isReplaceSpaces()) {
                javaUsername = javaUsername.replaceAll(" ", "_");
            }

            javaUniqueId = Utils.getJavaUuid(bedrockData.getXuid());
            this.bedrockIp = bedrockData.getIp();
        }

        this.javaUsername = javaUsername;
        this.javaUniqueId = javaUniqueId;
    }

    @Override
    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    @Override
    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }
}
