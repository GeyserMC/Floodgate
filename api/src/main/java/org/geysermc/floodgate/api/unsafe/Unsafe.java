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

package org.geysermc.floodgate.api.unsafe;

import java.util.UUID;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

/**
 * @deprecated The Floodgate API has been deprecated in favor of the GeyserApi, which is shared between Geyser
 * and Floodgate
 */
@Deprecated(forRemoval = true, since = "3.0.0")
public interface Unsafe {
    /**
     * Send a raw Bedrock packet to the given online Bedrock player.
     *
     * @param bedrockPlayer the uuid of the online Bedrock player
     * @param packetId      the id of the packet to send
     * @param packetData    the raw packet data
     */
    void sendPacket(UUID bedrockPlayer, int packetId, byte[] packetData);

    /**
     * Send a raw Bedrock packet to the given online Bedrock player.
     *
     * @param player     the Bedrock player to send the packet to
     * @param packetId   the id of the packet to send
     * @param packetData the raw packet data
     */
    default void sendPacket(FloodgatePlayer player, int packetId, byte[] packetData) {
        sendPacket(player.getCorrectUniqueId(), packetId, packetData);
    }
}
