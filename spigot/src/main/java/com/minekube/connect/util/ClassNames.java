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

package com.minekube.connect.util;

import static com.minekube.connect.util.ReflectionUtils.castedStaticBooleanValue;
import static com.minekube.connect.util.ReflectionUtils.getBooleanValue;
import static com.minekube.connect.util.ReflectionUtils.getClassSilently;
import static com.minekube.connect.util.ReflectionUtils.getField;
import static com.minekube.connect.util.ReflectionUtils.getFieldOfType;
import static com.minekube.connect.util.ReflectionUtils.getMethod;
import static com.minekube.connect.util.ReflectionUtils.getValue;
import static com.minekube.connect.util.ReflectionUtils.invoke;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
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
    public static final Field VELOCITY_LOGIN_MESSAGE_ID;
    public static final Field LOGIN_PROFILE;
    public static final Field PACKET_LISTENER;

    @Nullable public static final Field PAPER_DISABLE_USERNAME_VALIDATION;
    @Nullable public static final BooleanSupplier PAPER_VELOCITY_SUPPORT;

    public static final Method GET_PROFILE_METHOD;
    public static final Method LOGIN_DISCONNECT;
    public static final Method NETWORK_EXCEPTION_CAUGHT;
    public static final Method INIT_UUID;
    public static final Method FIRE_LOGIN_EVENTS;

    public static final Field BUNGEE;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        SPIGOT_MAPPING_PREFIX = "net.minecraft.server." + version;

        // SpigotSkinApplier
        Class<?> craftPlayerClass = ReflectionUtils.getClass(
                "org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
        GET_PROFILE_METHOD = getMethod(craftPlayerClass, "getProfile");
        checkNotNull(GET_PROFILE_METHOD, "Get profile method");

        String nmsPackage = SPIGOT_MAPPING_PREFIX + '.';

        // SpigotInjector
        MINECRAFT_SERVER = getClassOrFallback(
                "net.minecraft.server.MinecraftServer",
                nmsPackage + "MinecraftServer"
        );

        SERVER_CONNECTION = getClassOrFallback(
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
        Class<?> networkManager = getClassOrFallback(
                "net.minecraft.network.NetworkManager",
                nmsPackage + "NetworkManager"
        );

        SOCKET_ADDRESS = getFieldOfType(networkManager, SocketAddress.class, false);

        HANDSHAKE_PACKET = getClassOrFallback(
                "net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol",
                nmsPackage + "PacketHandshakingInSetProtocol"
        );

        HANDSHAKE_HOST = getFieldOfType(HANDSHAKE_PACKET, String.class);
        checkNotNull(HANDSHAKE_HOST, "Handshake host");

        LOGIN_START_PACKET = getClassOrFallback(
                "net.minecraft.network.protocol.login.PacketLoginInStart",
                nmsPackage + "PacketLoginInStart"
        );

        LOGIN_LISTENER = getClassOrFallback(
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

        VELOCITY_LOGIN_MESSAGE_ID = getField(LOGIN_LISTENER, "velocityLoginMessageId");

        // there are multiple no-arg void methods
        INIT_UUID = getMethod(LOGIN_LISTENER, "initUUID");
        checkNotNull(INIT_UUID, "initUUID from LoginListener");

        Class<?> packetListenerClass = getClassOrFallback(
                "net.minecraft.network.PacketListener",
                nmsPackage + "PacketListener"
        );
        PACKET_LISTENER = getFieldOfType(networkManager, packetListenerClass);
        checkNotNull(PACKET_LISTENER, "Packet listener");

        LOGIN_HANDLER = getClassOrFallback(
                "net.minecraft.server.network.LoginListener$LoginHandler",
                nmsPackage + "LoginListener$LoginHandler"
        );

        LOGIN_HANDLER_CONSTRUCTOR =
                ReflectionUtils.getConstructor(LOGIN_HANDLER, true, LOGIN_LISTENER);
        checkNotNull(LOGIN_HANDLER_CONSTRUCTOR, "LoginHandler constructor");

        FIRE_LOGIN_EVENTS = getMethod(LOGIN_HANDLER, "fireEvents");
        checkNotNull(FIRE_LOGIN_EVENTS, "fireEvents from LoginHandler");

        PAPER_DISABLE_USERNAME_VALIDATION = getField(LOGIN_LISTENER,
                "iKnowThisMayNotBeTheBestIdeaButPleaseDisableUsernameValidation");

        if (Constants.DEBUG_MODE) {
            System.out.println("Paper disable username validation field exists? " +
                    (PAPER_DISABLE_USERNAME_VALIDATION != null));
        }

        // ProxyUtils
        Class<?> spigotConfig = ReflectionUtils.getClass("org.spigotmc.SpigotConfig");
        checkNotNull(spigotConfig, "Spigot config");

        BUNGEE = getField(spigotConfig, "bungee");
        checkNotNull(BUNGEE, "Bungee field");

        Class<?> paperConfigNew = getClassSilently(
                "io.papermc.paper.configuration.GlobalConfiguration");
        if (paperConfigNew != null) {
            // 1.19 and later
            Method paperConfigGet = checkNotNull(getMethod(paperConfigNew, "get"),
                    "GlobalConfiguration get");
            Field paperConfigProxies = checkNotNull(getField(paperConfigNew, "proxies"),
                    "Proxies field");
            Field paperConfigVelocity = checkNotNull(
                    getField(paperConfigProxies.getType(), "velocity"),
                    "velocity field");
            Field paperVelocityEnabled = checkNotNull(
                    getField(paperConfigVelocity.getType(), "enabled"),
                    "Velocity enabled field");
            PAPER_VELOCITY_SUPPORT = () -> {
                Object paperConfigInstance = invoke(null, paperConfigGet);
                Object proxiesInstance = getValue(paperConfigInstance, paperConfigProxies);
                Object velocityInstance = getValue(proxiesInstance, paperConfigVelocity);
                return getBooleanValue(velocityInstance, paperVelocityEnabled);
            };
        } else {
            // Pre-1.19
            Class<?> paperConfig = getClassSilently(
                    "com.destroystokyo.paper.PaperConfig");

            if (paperConfig != null) {
                Field velocitySupport = getField(paperConfig, "velocitySupport");
                // velocitySupport field is null pre-1.13
                PAPER_VELOCITY_SUPPORT = velocitySupport != null ?
                        () -> castedStaticBooleanValue(velocitySupport) : null;
            } else {
                PAPER_VELOCITY_SUPPORT = null;
            }
        }
    }

    private static Class<?> getClassOrFallback(String className, String fallbackName) {
        Class<?> clazz = getClassSilently(className);

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

    private static <T> T checkNotNull(@CheckForNull T toCheck, @CheckForNull String objectName) {
        return Preconditions.checkNotNull(toCheck, objectName + " cannot be null");
    }
}
