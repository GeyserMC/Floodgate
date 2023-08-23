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

package org.geysermc.floodgate.core.addon.data;

import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.connection.FloodgateConnectionBuilder;
import org.geysermc.floodgate.core.util.Utils;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;

@Getter
public class HandshakeDataImpl implements HandshakeData {
    private final Channel channel;
    private final boolean floodgatePlayer;
    private final BedrockData bedrockData;
    private final String javaUsername;
    private final UUID javaUniqueId;

    @Setter private LinkedPlayer linkedPlayer;
    @Setter private String hostname;
    @Setter private String ip;
    @Setter private String disconnectReason;

    public HandshakeDataImpl(
            Channel channel,
            FloodgateConnection connection,
            String hostname
    ) {
        this.channel = channel;
        this.floodgatePlayer = connection != null;
        this.hostname = hostname;

        BedrockData bedrockData = null;
        LinkedPlayer linkedPlayer = null;
        String javaUsername = null;
        UUID javaUniqueId = null;

        if (connection != null) {
            bedrockData = connection.toBedrockData();
            linkedPlayer = connection.linkedPlayer();

            javaUsername = connection.javaUsername();
            javaUniqueId = Utils.getJavaUuid(bedrockData.getXuid());
            this.ip = bedrockData.getIp();
        }

        this.bedrockData = bedrockData;
        this.linkedPlayer = linkedPlayer;
        this.javaUsername = javaUsername;
        this.javaUniqueId = javaUniqueId;
    }

    public FloodgateConnection applyChanges(FloodgateConnection connection, String hostname, FloodgateConfig config) {
        var newLink = !Objects.equals(connection.linkedPlayer(), this.linkedPlayer) ? this.linkedPlayer : null;

        var thisIp = convertIp(this.ip);
        var newIp = !connection.ip().equals(thisIp) ? thisIp : null;

        // only change when there have been any changes
        if (newLink == null && newIp == null) {
            return connection;
        }

        var builder = new FloodgateConnectionBuilder(config);
        connection.fillBuilder(builder);
        if (newLink != null) builder.linkedPlayer(newLink);
        if (newIp != null) builder.ip(newIp);
        return builder.build();
    }

    @Override
    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.getJavaUsername() : javaUsername;
    }

    @Override
    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.getJavaUniqueId() : javaUniqueId;
    }

    private static InetAddress convertIp(String ip) {
        String[] sections = ip.split("\\.");
        // if not ipv4, expect ipv6
        if (sections.length == 1) {
            sections = ip.split(":");
        }

        byte[] addressBytes = new byte[sections.length];
        for (int i = 0; i < sections.length; i++) {
            addressBytes[i] = (byte) Integer.parseInt(sections[i]);
        }

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException exception) {
            throw new RuntimeException(exception);
        }
    }
}
