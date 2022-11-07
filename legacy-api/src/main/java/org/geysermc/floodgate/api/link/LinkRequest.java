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

package org.geysermc.floodgate.api.link;

import java.util.UUID;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public interface LinkRequest {
    /**
     * Returns the Java username of the linked player.
     */
    String getJavaUsername();

    /**
     * Returns the Java unique id of the linked player.
     */
    UUID getJavaUniqueId();

    /**
     * Returns the code that the Bedrock player has to enter in order to link the account.
     */
    String getLinkCode();

    /**
     * Returns the username of player being linked.
     */
    String getBedrockUsername();

    /**
     * Returns the unix time when the player link was requested.
     */
    long getRequestTime();

    /**
     * If this player link request is expired.
     *
     * @param linkTimeout the link timeout in millis
     * @return true if the difference between now and requestTime is greater then the link timout
     */
    boolean isExpired(long linkTimeout);

    /**
     * Checks if the given FloodgatePlayer is the player requested in this LinkRequest. This method
     * will check both the real bedrock username {@link FloodgatePlayer#getUsername()} and the
     * edited username {@link FloodgatePlayer#getJavaUsername()} and returns true if one of the two
     * matches.
     *
     * @param player the player to check
     * @return true if the given player is the player requested
     */
    default boolean isRequestedPlayer(FloodgatePlayer player) {
        return getBedrockUsername().equals(player.getUsername()) ||
                getBedrockUsername().equals(player.getJavaUsername());
    }
}
