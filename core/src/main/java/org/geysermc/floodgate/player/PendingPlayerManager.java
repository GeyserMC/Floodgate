/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.player;

import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

@Singleton
public class PendingPlayerManager {

    private final Map<InetSocketAddress, FloodgatePlayer> pendingByAddress = new ConcurrentHashMap<>();
    private final Map<String, FloodgatePlayer> pendingByUsername = new ConcurrentHashMap<>();

    /**
     * Register a pending player after successful handshake decryption.
     * Called from platform-specific handshake handlers.
     *
     * @param address the connection address
     * @param player the FloodgatePlayer instance
     */
    public void add(InetSocketAddress address, FloodgatePlayer player) {
        pendingByAddress.put(address, player);
        // Store by raw username (the original Bedrock gamertag, without prefix)
        pendingByUsername.put(player.getUsername().toLowerCase(), player);
    }

    /**
     * Remove a pending player after login completion or disconnect.
     *
     * @param address the connection address
     */
    public void remove(InetSocketAddress address) {
        FloodgatePlayer player = pendingByAddress.remove(address);
        if (player != null) {
            pendingByUsername.remove(player.getUsername().toLowerCase());
        }
    }

    /**
     * Remove a pending player by username.
     *
     * @param rawUsername the raw username without prefix
     */
    public void removeByUsername(String rawUsername) {
        if (rawUsername == null) return;
        FloodgatePlayer player = pendingByUsername.remove(rawUsername.toLowerCase());
        if (player != null) {
            // Find and remove from address map
            pendingByAddress.entrySet().removeIf(entry ->
                    entry.getValue().getUsername().equalsIgnoreCase(rawUsername));
        }
    }

    /**
     * Get a pending player by connection address.
     *
     * @param address the connection address
     * @return the FloodgatePlayer or null if not found
     */
    public FloodgatePlayer get(InetSocketAddress address) {
        return pendingByAddress.get(address);
    }

    /**
     * Get a pending player by raw username (without prefix).
     *
     * @param rawUsername the raw username
     * @return the FloodgatePlayer or null if not found
     */
    public FloodgatePlayer getByUsername(String rawUsername) {
        if (rawUsername == null) return null;
        return pendingByUsername.get(rawUsername.toLowerCase());
    }
}
