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

package com.minekube.connect.api;

import com.minekube.connect.api.player.ConnectPlayer;
import java.util.Collection;
import java.util.UUID;

public interface ConnectApi {
    /**
     * Returns the Connect API instance.
     */
    static ConnectApi getInstance() {
        return InstanceHolder.getApi();
    }

    /**
     * Returns all the online Connect players.
     */
    Collection<ConnectPlayer> getPlayers();

    /**
     * Returns the number of Connect players who are currently online.
     */
    int getPlayerCount();

    /**
     * Method to determine if the given <b>online</b> player is a bedrock player
     *
     * @param uuid The uuid of the <b>online</b> player
     * @return true if the given <b>online</b> player is tunneled by Connect
     */
    boolean isConnectPlayer(UUID uuid);

    /**
     * Get info about the given player.
     *
     * @param uuid the uuid of the <b>online</b> player
     * @return ConnectPlayer if the given uuid is a player tunneled by Connect
     */
    ConnectPlayer getPlayer(UUID uuid);
}
