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

import static org.geysermc.floodgate.util.ReflectionUtils.getFieldOfType;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

@SuppressWarnings("PMD.SystemPrintln")
public class ClassNames {
    public static final String SPIGOT_MAPPING_PREFIX;

    public static final Class<?> MINECRAFT_SERVER;
    public static final Class<?> SERVER_CONNECTION;
    public static final Class<?> HANDSHAKE_PACKET;
    public static final Class<?> LOGIN_START_PACKET;
    public static final Class<?> LOGIN_LISTENER;
    public static final Class<?> LOGIN_HANDLER;

    public static final Constructor<OfflinePlayer> CRAFT_OFFLINE_PLAYER_CONSTRUCTOR;
    public static final Constructor<?> LOGIN_HANDLER_CONSTRUCTOR;

    public static final Field SOCKET_ADDRESS;
    public static final Field HANDSHAKE_HOST;
    public static final Field LOGIN_PROFILE;
    public static final Field PACKET_LISTENER;

    public static final Method GET_PROFILE_METHOD;
    public static final Method LOGIN_DISCONNECT;
    public static final Method NETWORK_EXCEPTION_CAUGHT;
    public static final Method INIT_UUID;
    public static final Method FIRE_LOGIN_EVENTS;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        SPIGOT_MAPPING_PREFIX = "net.minecraft.server." + version;


        // SpigotSkinApplier
        Class<?> craftPlayerClass = ReflectionUtils.getClass(
                "org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
        GET_PROFILE_METHOD = ReflectionUtils.getMethod(craftPlayerClass, "getProfile");
        checkNotNull(GET_PROFILE_METHOD, "Get profile method");

        String nmsPackage = SPIGOT_MAPPING_PREFIX + '.';


        // SpigotInjector
        MINECRAFT_SERVER = getClassOrFallBack(
                "net.minecraft.server.MinecraftServer",
                nmsPackage + "MinecraftServer"
        );

        SERVER_CONNECTION = getClassOrFallBack(
                "net.minecraft.server.network.ServerConnection",
                nmsPackage + "ServerConnection"
        );

        // WhitelistUtils
        Class<?> craftServerClass = ReflectionUtils.getClass(
                "org.bukkit.craftbukkit." + version + ".CraftServer");
        Class<OfflinePlayer> craftOfflinePlayerClass = ReflectionUtils.getCastedClass(
                "org.bukkit.craftbukkit." + version + ".CraftOfflinePlayer");

        CRAFT_OFFLINE_PLAYER_CONSTRUCTOR = ReflectionUtils.getConstructor(
                craftOfflinePlayerClass, true, craftServerClass, GameProfile.class);

        // SpigotDataHandler
        Class<?> networkManager = getClassOrFallBack(
                "net.minecraft.network.NetworkManager",
                nmsPackage + "NetworkManager"
        );

        SOCKET_ADDRESS = getFieldOfType(networkManager, SocketAddress.class, false);

        HANDSHAKE_PACKET = getClassOrFallBack(
                "net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol",
                nmsPackage + "PacketHandshakingInSetProtocol"
        );

        HANDSHAKE_HOST = getFieldOfType(HANDSHAKE_PACKET, String.class);
        checkNotNull(HANDSHAKE_HOST, "Handshake host");

        LOGIN_START_PACKET = getClassOrFallBack(
                "net.minecraft.network.protocol.login.PacketLoginInStart",
                nmsPackage + "PacketLoginInStart"
        );

        LOGIN_LISTENER = getClassOrFallBack(
                "net.minecraft.server.network.LoginListener",
                nmsPackage + "LoginListener"
        );

        LOGIN_PROFILE = getFieldOfType(LOGIN_LISTENER, GameProfile.class);
        checkNotNull(LOGIN_PROFILE, "Profile from LoginListener");

        LOGIN_DISCONNECT = getMethod(LOGIN_LISTENER, "disconnect", String.class);
        checkNotNull(LOGIN_DISCONNECT, "LoginListener's disconnect method");

        NETWORK_EXCEPTION_CAUGHT = getMethod(
                networkManager,
                "exceptionCaught",
                ChannelHandlerContext.class, Throwable.class
        );

        // there are multiple no-arg void methods
        INIT_UUID = getMethod(LOGIN_LISTENER, "initUUID");
        checkNotNull(INIT_UUID, "initUUID from LoginListener");

        Class<?> packetListenerClass = getClassOrFallBack(
                "net.minecraft.network.PacketListener",
                nmsPackage + "PacketListener"
        );
        PACKET_LISTENER = getFieldOfType(networkManager, packetListenerClass);
        checkNotNull(PACKET_LISTENER, "Packet listener");

        LOGIN_HANDLER = getClassOrFallBack(
                "net.minecraft.server.network.LoginListener$LoginHandler",
                nmsPackage + "LoginListener$LoginHandler"
        );

        LOGIN_HANDLER_CONSTRUCTOR =
                ReflectionUtils.getConstructor(LOGIN_HANDLER, true, LOGIN_LISTENER);
        checkNotNull(LOGIN_HANDLER_CONSTRUCTOR, "LoginHandler constructor");

        FIRE_LOGIN_EVENTS = getMethod(LOGIN_HANDLER, "fireEvents");
        checkNotNull(FIRE_LOGIN_EVENTS, "fireEvents from LoginHandler");
    }

    private static Class<?> getClassOrFallBack(String className, String fallbackName) {
        Class<?> clazz = ReflectionUtils.getClassSilently(className);

        if (clazz != null) {
            if (Constants.DEBUG_MODE) {
                System.out.println("Found class (primary): " + clazz.getName());
            }
            return clazz;
        }

        // do throw an exception when both classes couldn't be found
        clazz = ReflectionUtils.getClassOrThrow(fallbackName);
        if (Constants.DEBUG_MODE) {
            System.out.println("Found class (fallback): " + clazz.getName());
        }

        return clazz;
    }

    private static void checkNotNull(Object toCheck, String objectName) {
        Preconditions.checkNotNull(toCheck, objectName + " cannot be null");
    }
}
