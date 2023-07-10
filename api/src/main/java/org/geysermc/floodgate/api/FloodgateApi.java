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

package org.geysermc.floodgate.api;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.event.FloodgateEventBus;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.unsafe.Unsafe;

public interface FloodgateApi {
    /**
     * Returns the Floodgate API instance.
     */
    static FloodgateApi getInstance() {
        return InstanceHolder.getApi();
    }

    /**
     * Returns the character(s) that will be added in front of a Bedrock player's name to prevent
     * username duplicates.
     */
    String getPlayerPrefix();

    /**
     * Returns all the online Floodgate players.
     */
    Collection<FloodgatePlayer> getPlayers();

    /**
     * Returns the number of Floodgate players who are currently online.
     */
    int getPlayerCount();

    /**
     * Method to determine if the given <b>online</b> player is a bedrock player
     *
     * @param uuid The uuid of the <b>online</b> player
     * @return true if the given <b>online</b> player is a Bedrock player
     */
    boolean isFloodgatePlayer(UUID uuid);

    /**
     * Get info about the given Bedrock player
     *
     * @param uuid the uuid of the <b>online</b> Bedrock player
     * @return FloodgatePlayer if the given uuid is a Bedrock player
     */
    FloodgatePlayer getPlayer(UUID uuid);

    /**
     * Create a valid Java player uuid of a xuid
     *
     * @param xuid the xuid that should be converted
     * @return the created uuid based of the given xuid
     */
    UUID createJavaPlayerId(long xuid);

    /**
     * Checks if the uuid of the player has the {@link #createJavaPlayerId(long)} format. This
     * method can't validate a linked player uuid, since that doesn't equal the format. Use
     * {@link #isFloodgatePlayer(UUID)} if you want to include linked accounts.
     *
     * @param uuid the uuid to check
     * @return true if the given uuid has the correct format.
     */
    boolean isFloodgateId(UUID uuid);

    boolean sendForm(UUID uuid, Form form);

    boolean sendForm(UUID uuid, FormBuilder<?, ?, ?> formBuilder);

    boolean transferPlayer(UUID uuid, String address, int port);

    /**
     * Get the xuid of the user that has the given gamertag.
     *
     * @param gamertag the gamertag of the player
     * @return the xuid of the player with the given gamertag, or null when there is no player with
     * the given gamertag
     */
    CompletableFuture<Long> getXuidFor(String gamertag);

    /**
     * Get the xuid of the player that has the given gamertag. It does the same thing as
     * {@link #getXuidFor(String)} except that this method will return the xuid in Floodgate uuid
     * format instead of just a long
     *
     * @param gamertag the gamertag of the player
     * @return the xuid of the player with the given gamertag, or null when there is no player with
     * the given gamertag
     */
    default CompletableFuture<UUID> getUuidFor(String gamertag) {
        return getXuidFor(gamertag).thenApply(xuid -> {
            if (xuid == null) {
                return null;
            }
            return createJavaPlayerId(xuid);
        });
    }

    /**
     * Get the gamertag of the user that has the given xuid.
     *
     * @param xuid the gamertag of the player
     * @return the gamertag of the player with the given xuid, or null when there is not player with
     * the given xuid
     */
    CompletableFuture<String> getGamertagFor(long xuid);

    default FloodgateEventBus getEventBus() {
        return InstanceHolder.getEventBus();
    }

    /**
     * Returns the instance that manages all the linking.
     */
    default PlayerLink getPlayerLink() {
        return InstanceHolder.getPlayerLink();
    }

    Unsafe unsafe();
}
