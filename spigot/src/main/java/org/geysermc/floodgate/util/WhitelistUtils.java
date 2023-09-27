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

package org.geysermc.floodgate.util;

import static org.bukkit.Bukkit.getServer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.geysermc.floodgate.FloodgatePlatform;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.FloodgateApi;



@SuppressWarnings("ConstantConditions")
public final class WhitelistUtils {

    /**
     * Whitelist the given Bedrock player.
     *
     * @param uuid     the UUID of the Bedrock player to be whitelisted
     * @param username the username of the Bedrock player to be whitelisted
     * @return true if the player has been whitelisted, false if the player is already whitelisted
     */
    public static boolean addPlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);

        OfflinePlayer player = ReflectionUtils.newInstance(
                ClassNames.CRAFT_OFFLINE_PLAYER_CONSTRUCTOR,
                getServer(), profile
        );
        if (player.isWhitelisted()) {
            return false;
        }

        Bukkit.getScheduler().runTask(getServer().getPluginManager().getPlugin("floodgate"),
                new Runnable() {
                    @Override
                    public void run() {
                        player.setWhitelisted(true);
                    }
                });
        return true;
    }

    /**
     * Removes the given Bedrock player from the whitelist.
     *
     * @param uuid     the UUID of the Bedrock player to be removed
     * @param username the username of the Bedrock player to be removed
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     * whitelisted
     */
    public static boolean removePlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);

        OfflinePlayer player = ReflectionUtils.newInstance(
                ClassNames.CRAFT_OFFLINE_PLAYER_CONSTRUCTOR,
                getServer(), profile
        );
        if (!player.isWhitelisted()) {
            return false;
        }

        Bukkit.getScheduler().runTask(getServer().getPluginManager().getPlugin("floodgate"),
                new Runnable() {
                    @Override
                    public void run() {
                        player.setWhitelisted(false);
                    }
                });

        return true;
    }
}
