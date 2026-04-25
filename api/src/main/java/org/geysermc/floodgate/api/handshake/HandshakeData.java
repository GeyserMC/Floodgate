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

package org.geysermc.floodgate.api.handshake;

import io.netty.channel.Channel;
import java.util.UUID;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;

/**
 * @deprecated Handshake handlers will be removed with the launch of Floodgate 3.0. Please look at
 * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
 */
@Deprecated
public interface HandshakeData {
    /**
     * Returns the Channel holding the connection between the client and the server.
     */
    Channel getChannel();

    /**
     * Returns true if the given player is a Floodgate player, false otherwise.
     */
    boolean isFloodgatePlayer();

    /**
     * Returns the decrypted BedrockData sent by Geyser or null if the player isn't a Floodgate
     * player.
     */
    BedrockData getBedrockData();

    String getJavaUsername();

    String getCorrectUsername();

    UUID getJavaUniqueId();

    UUID getCorrectUniqueId();

    /**
     * Returns the linked account associated with the client or null if the player isn't linked or
     * not a Floodgate player.
     */
    LinkedPlayer getLinkedPlayer();

    /**
     * Set the LinkedPlayer. This will be ignored if the player isn't a Floodgate player
     *
     * @param player the player to use as link
     */
    void setLinkedPlayer(LinkedPlayer player);

    /**
     * Returns the hostname used in the handshake packet. This is the hostname after Floodgate
     * removed the data.
     */
    String getHostname();

    /**
     * Set the hostname of the handshake packet. Changing it here will also change it in the
     * handshake packet.
     *
     * @param hostname the new hostname
     */
    void setHostname(String hostname);

    /**
     * Returns the IP address of the client. The initial value is {@link BedrockData#getIp()} when
     * BedrockData isn't null, or null if BedrockData is null. This method will return the changed
     * IP if it has been changed using {@link #setIp(String)}
     */
    String getIp();

    /**
     * Set the IP address of the connected client. Floodgate doesn't perform any checks if the
     * provided data is valid (hence one of the reasons why this class has been made for advanced
     * users), thank you for not abusing Floodgate's trust in you :)
     *
     * @param address the IP address of the client
     */
    void setIp(String address);

    /**
     * Returns the reason to disconnect the current player.
     */
    String getDisconnectReason();

    /**
     * Set the reason to disconnect the current player.
     *
     * @param reason the reason to disconnect
     */
    void setDisconnectReason(String reason);

    /**
     * Returns if the player should be disconnected
     */
    default boolean shouldDisconnect() {
        return getDisconnectReason() != null;
    }
}
