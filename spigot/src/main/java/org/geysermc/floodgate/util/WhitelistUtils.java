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

import static com.google.common.base.Preconditions.checkNotNull;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;

@SuppressWarnings("ConstantConditions")
public final class WhitelistUtils {
    private static final Method GET_SERVER;
    private static final Method GET_PLAYER_LIST;
    private static final Method GET_WHITELIST;
    private static final Method IS_WHITELISTED;
    private static final Constructor<?> WHITELIST_ENTRY;
    private static final Method ADD_ENTRY;
    private static final Method REMOVE_ENTRY;

    static {
        Class<?> bukkitServerClass = Bukkit.getServer().getClass();
        GET_SERVER = ReflectionUtils.getMethod(bukkitServerClass, "getServer");
        checkNotNull(GET_SERVER, bukkitServerClass.getName() + " doesn't have a getServer method?");

        Class<?> minecraftServer = ReflectionUtils.getPrefixedClass("MinecraftServer");
        GET_PLAYER_LIST = ReflectionUtils.getMethod(minecraftServer, "getPlayerList");
        checkNotNull(GET_PLAYER_LIST, "Cannot find getPlayerList");

        Class<?> playerList = ReflectionUtils.getPrefixedClass("PlayerList");
        GET_WHITELIST = ReflectionUtils.getMethod(playerList, "getWhitelist");
        checkNotNull(GET_WHITELIST, "Cannot find getWhitelist");

        Class<?> whitelist = ReflectionUtils.getPrefixedClass("WhiteList");
        IS_WHITELISTED = ReflectionUtils.getMethod(whitelist, "isWhitelisted", GameProfile.class);
        checkNotNull(IS_WHITELISTED, "Couldn't find the isWhitelisted method!");

        Class<?> whitelistEntry = ReflectionUtils.getPrefixedClass("WhiteListEntry");
        WHITELIST_ENTRY = ReflectionUtils.getConstructor(whitelistEntry, GameProfile.class);
        checkNotNull(WHITELIST_ENTRY, "Could not find required WhiteListEntry constructor");

        Class<?> jsonList = ReflectionUtils.getPrefixedClass("JsonList");
        ADD_ENTRY = ReflectionUtils.getMethodByName(jsonList, "add", false);
        checkNotNull(ADD_ENTRY, "Cannot find add method");

        Class<?> jsonListEntry = ReflectionUtils.getPrefixedClass("JsonListEntry");
        REMOVE_ENTRY = ReflectionUtils.getMethodFromParam(jsonList, jsonListEntry, false);
        checkNotNull(REMOVE_ENTRY, "Cannot find remove method");
    }

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

        ReflectionUtils.invoke(whitelist, ADD_ENTRY, entry);
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

        Object entry = ReflectionUtils.newInstance(WHITELIST_ENTRY, profile);

        ReflectionUtils.invoke(whitelist, REMOVE_ENTRY, entry);
        return true;
    }

    private static Object getWhitelist() {
        Object minecraftServer = ReflectionUtils.invoke(Bukkit.getServer(), GET_SERVER);
        Object playerList = ReflectionUtils.invoke(minecraftServer, GET_PLAYER_LIST);
        return ReflectionUtils.invoke(playerList, GET_WHITELIST);
    }
}
