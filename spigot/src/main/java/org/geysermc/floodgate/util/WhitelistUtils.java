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

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

@SuppressWarnings("ConstantConditions")
public final class WhitelistUtils {

    /**
     * Whitelist the given Bedrock player.
     *
     * @param uuid     the UUID of the Bedrock player to be removed
     * @param username the username of the Bedrock player to be removed
     * @param versionSpecificMethods a reference to the SpigotVersionSpecificMethods used in SpigotCommandUtil
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     */
    public static boolean addPlayer(UUID uuid, String username, SpigotVersionSpecificMethods versionSpecificMethods) {
        GameProfile profile = new GameProfile(uuid, username);

        OfflinePlayer player = getOfflinePlayer(profile);
        if (player.isWhitelisted()) {
            return false;
        }
        setWhitelist(player, true, versionSpecificMethods);
        return true;
    }

    /**
     * Removes the given Bedrock player from the whitelist.
     *
     * @param uuid     the UUID of the Bedrock player to be removed
     * @param username the username of the Bedrock player to be removed
     * @param versionSpecificMethods a reference to the SpigotVersionSpecificMethods used in SpigotCommandUtil
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     * whitelisted
     */
    public static boolean removePlayer(UUID uuid, String username, SpigotVersionSpecificMethods versionSpecificMethods) {
        GameProfile profile = new GameProfile(uuid, username);

        OfflinePlayer player = getOfflinePlayer(profile);
        if (!player.isWhitelisted()) {
            return false;
        }
        setWhitelist(player, false, versionSpecificMethods);
        return true;
    }

    static void setWhitelist(OfflinePlayer player, boolean whitelist, SpigotVersionSpecificMethods versionSpecificMethods) {
        versionSpecificMethods.maybeSchedule(() -> player.setWhitelisted(whitelist), true); // Whitelisting is on the global thread
    }

    static OfflinePlayer getOfflinePlayer(GameProfile profile) {
        if (ClassNames.CRAFT_NEW_OFFLINE_PLAYER_CONSTRUCTOR != null) {
            Object nameAndId = ReflectionUtils.newInstance(
                    ClassNames.NAME_AND_ID_CONSTRUCTOR,
                    profile
            );

            return ReflectionUtils.newInstance(
                    ClassNames.CRAFT_NEW_OFFLINE_PLAYER_CONSTRUCTOR,
                    Bukkit.getServer(), nameAndId
            );
        } else {
            return ReflectionUtils.newInstance(
                    ClassNames.CRAFT_OFFLINE_PLAYER_CONSTRUCTOR,
                    Bukkit.getServer(), profile
            );
        }
    }
}
