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

package org.geysermc.floodgate.util;

import static org.geysermc.floodgate.util.ClassNames.ADD_WHITELIST_ENTRY;
import static org.geysermc.floodgate.util.ClassNames.GET_PLAYER_LIST;
import static org.geysermc.floodgate.util.ClassNames.GET_SERVER;
import static org.geysermc.floodgate.util.ClassNames.GET_WHITELIST;
import static org.geysermc.floodgate.util.ClassNames.IS_WHITELISTED;
import static org.geysermc.floodgate.util.ClassNames.REMOVE_WHITELIST_ENTRY;
import static org.geysermc.floodgate.util.ClassNames.WHITELIST_ENTRY;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;

@SuppressWarnings("ConstantConditions")
public final class WhitelistUtils {
    /**
     * Whitelist the given Bedrock player.
     *
     * @param xuid     the xuid of the Bedrock player to be whitelisted
     * @param username the username of the Bedrock player to be whitelisted
     * @return true if the player has been whitelisted, false if the player is already whitelisted
     */
    public static boolean addPlayer(String xuid, String username) {
        Object whitelist = getWhitelist();

        GameProfile profile = new GameProfile(Utils.getJavaUuid(xuid), username);

        if (ReflectionUtils.castedInvoke(whitelist, IS_WHITELISTED, profile)) {
            return false;
        }

        Object entry = ReflectionUtils.newInstance(WHITELIST_ENTRY, profile);

        ReflectionUtils.invoke(whitelist, ADD_WHITELIST_ENTRY, entry);
        return true;
    }

    /**
     * Removes the given Bedrock player from the whitelist.
     *
     * @param xuid     the xuid of the Bedrock player to be removed
     * @param username the username of the Bedrock player to be removed
     * @return true if the player has been removed from the whitelist, false if the player wasn't
     * whitelisted
     */
    public static boolean removePlayer(String xuid, String username) {
        Object whitelist = getWhitelist();

        GameProfile profile = new GameProfile(Utils.getJavaUuid(xuid), username);

        if (!(boolean) ReflectionUtils.castedInvoke(whitelist, IS_WHITELISTED, profile)) {
            return false;
        }

        ReflectionUtils.invoke(whitelist, REMOVE_WHITELIST_ENTRY, profile);
        return true;
    }

    private static Object getWhitelist() {
        Object minecraftServer = ReflectionUtils.invoke(Bukkit.getServer(), GET_SERVER);
        Object playerList = ReflectionUtils.invoke(minecraftServer, GET_PLAYER_LIST);
        return ReflectionUtils.invoke(playerList, GET_WHITELIST);
    }
}
