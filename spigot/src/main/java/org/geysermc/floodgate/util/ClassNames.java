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

import static org.geysermc.floodgate.util.ReflectionUtils.castedStaticBooleanValue;
import static org.geysermc.floodgate.util.ReflectionUtils.getBooleanValue;
import static org.geysermc.floodgate.util.ReflectionUtils.getClassOrFallback;
import static org.geysermc.floodgate.util.ReflectionUtils.getClassOrFallbackSilently;
import static org.geysermc.floodgate.util.ReflectionUtils.getClassSilently;
import static org.geysermc.floodgate.util.ReflectionUtils.getConstructor;
import static org.geysermc.floodgate.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.util.ReflectionUtils.getFieldOfType;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.util.ReflectionUtils.getValue;
import static org.geysermc.floodgate.util.ReflectionUtils.invoke;
import static org.geysermc.floodgate.util.ReflectionUtils.makeAccessible;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckForNull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("PMD.SystemPrintln")
public class ClassNames {
    public static final Class<?> MINECRAFT_SERVER;
    public static final Class<?> SERVER_CONNECTION;
    public static final Class<?> HANDSHAKE_PACKET;
    public static final Class<?> LOGIN_START_PACKET;
    public static final Class<?> LOGIN_LISTENER;
    @Nullable public static final Class<?> CLIENT_INTENT;

    @Nullable public static final Constructor<OfflinePlayer> CRAFT_OFFLINE_PLAYER_CONSTRUCTOR;
    @Nullable public static final Constructor<OfflinePlayer> CRAFT_NEW_OFFLINE_PLAYER_CONSTRUCTOR;
    @Nullable public static final Constructor<?> NAME_AND_ID_CONSTRUCTOR;
    @Nullable public static final Constructor<?> LOGIN_HANDLER_CONSTRUCTOR;
    @Nullable public static final Constructor<?> HANDSHAKE_PACKET_CONSTRUCTOR;

    public static final Field SOCKET_ADDRESS;
    public static final Field HANDSHAKE_HOST;
    public static final Field LOGIN_PROFILE;
    public static final Field PACKET_LISTENER;

    @Nullable public static final Field HANDSHAKE_PORT;
    @Nullable public static final Field HANDSHAKE_PROTOCOL;
    @Nullable public static final Field HANDSHAKE_INTENTION;

    @Nullable public static final Field PAPER_DISABLE_USERNAME_VALIDATION;
    @Nullable public static final BooleanSupplier PAPER_VELOCITY_SUPPORT;

    public static final Method GET_PROFILE_METHOD;

    @Nullable public static final Method GET_ENTITY_HUMAN_METHOD;
    @Nullable public static final Field GAME_PROFILE_FIELD;

    public static final Method LOGIN_DISCONNECT;
    public static final Method NETWORK_EXCEPTION_CAUGHT;
    @Nullable public static final Method INIT_UUID;
    @Nullable public static final Method FIRE_LOGIN_EVENTS;
    @Nullable public static final Method FIRE_LOGIN_EVENTS_GAME_PROFILE;
    @Nullable public static final Method CALL_PLAYER_PRE_LOGIN_EVENTS;
    @Nullable public static final Method START_CLIENT_VERIFICATION;

    public static final Field BUNGEE;

    public static final boolean IS_FOLIA;
    public static final boolean IS_PRE_1_20_2;
    public static final boolean IS_POST_LOGIN_HANDLER;

    static {
        // ahhhhhhh, this class should really be reworked at this point

        String[] versionSplit = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        // Paper, since 1.20.5, no longer relocates CraftBukkit classes
        // and NMS classes aren't relocated for a few versions now (both Spigot & Paper)
        if (versionSplit.length <= 3 && getClassSilently("net.minecraft.server.MinecraftServer") == null) {
            throw new IllegalStateException(
                    "Was unable to find net.minecraft.server.MinecraftServer. " +
                    "We don't support Mojmap yet"
            );
        }
        // Makes it that we don't have to lookup both the new and the old
        // 'org.bukkit.craftbukkit. + version + CraftPlayer' will be .CraftPlayer on new
        // versions and .v1_8R3.CraftPlayer on older versions
        String version = versionSplit.length > 3 ? versionSplit[3] + '.' : "";
        String nmsPackage = "net.minecraft.server." + version;


        // SpigotSkinApplier
        Class<?> craftPlayerClass = getClassOrFallback(
                "org.bukkit.craftbukkit.entity.CraftPlayer",
                "org.bukkit.craftbukkit." + version + "entity.CraftPlayer"
        );
        GET_PROFILE_METHOD = getMethod(craftPlayerClass, "getProfile");
        checkNotNull(GET_PROFILE_METHOD, "Get profile method");

        GET_ENTITY_HUMAN_METHOD = getMethod(craftPlayerClass, "getHandle");
        Class<?> entityHumanClass = getClassOrFallbackSilently(
                "net.minecraft.world.entity.player.EntityHuman",
                "net.minecraft.world.entity.player.Player"
        );
        if (entityHumanClass != null) {
            // Spigot obfuscates field name
            GAME_PROFILE_FIELD = getFieldOfType(entityHumanClass, GameProfile.class);
        } else {
            GAME_PROFILE_FIELD = null;
        }

        // SpigotInjector
        MINECRAFT_SERVER = getClassOrFallback(
                "net.minecraft.server.MinecraftServer",
                nmsPackage + "MinecraftServer"
        );

        SERVER_CONNECTION = getClassOrFallback(
                "net.minecraft.server.network.ServerConnectionListener",
                "net.minecraft.server.network.ServerConnection",
                nmsPackage + "ServerConnection"
        );

        // WhitelistUtils
        Class<?> craftServerClass = getClassOrFallback(
                "org.bukkit.craftbukkit.CraftServer",
                "org.bukkit.craftbukkit." + version + "CraftServer"
        );
        Class<OfflinePlayer> craftOfflinePlayerClass = ReflectionUtils.getCastedClassOrFallback(
                "org.bukkit.craftbukkit.CraftOfflinePlayer",
                "org.bukkit.craftbukkit." + version + "CraftOfflinePlayer"
        );

        CRAFT_OFFLINE_PLAYER_CONSTRUCTOR = ReflectionUtils.getConstructor(
                craftOfflinePlayerClass, true, craftServerClass, GameProfile.class);

        if (CRAFT_OFFLINE_PLAYER_CONSTRUCTOR == null) { // Changed in 1.21.9
            Class<?> nameAndIdClass = getClassSilently("net.minecraft.server.players.NameAndId");

            CRAFT_NEW_OFFLINE_PLAYER_CONSTRUCTOR = ReflectionUtils.getConstructor(
                    craftOfflinePlayerClass, true, craftServerClass, nameAndIdClass);

            NAME_AND_ID_CONSTRUCTOR = ReflectionUtils.getConstructor(nameAndIdClass, true, GameProfile.class);
        } else {
            CRAFT_NEW_OFFLINE_PLAYER_CONSTRUCTOR = null;
            NAME_AND_ID_CONSTRUCTOR = null;
        }

        // SpigotDataHandler
        Class<?> networkManager = getClassOrFallback(
                "net.minecraft.network.Connection",
                "net.minecraft.network.NetworkManager",
                nmsPackage + "NetworkManager"
        );

        SOCKET_ADDRESS = getFieldOfType(networkManager, SocketAddress.class, false);

        HANDSHAKE_PACKET = getClassOrFallback(
                "net.minecraft.network.protocol.handshake.ClientIntentionPacket",
                "net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol",
                nmsPackage + "PacketHandshakingInSetProtocol"
        );

        HANDSHAKE_HOST = getFieldOfType(HANDSHAKE_PACKET, String.class);
        checkNotNull(HANDSHAKE_HOST, "Handshake host");

        LOGIN_START_PACKET = getClassOrFallback(
                "net.minecraft.network.protocol.login.ServerboundHelloPacket",
                "net.minecraft.network.protocol.login.PacketLoginInStart",
                nmsPackage + "PacketLoginInStart"
        );

        LOGIN_LISTENER = getClassOrFallback(
                "net.minecraft.server.network.ServerLoginPacketListenerImpl",
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
        // Pre 1.20.2 uses initUUID so if it's null, we're on 1.20.2 or later
        INIT_UUID = getMethod(LOGIN_LISTENER, "initUUID");
        IS_PRE_1_20_2 = INIT_UUID != null;

        // somewhere during 1.20.4 md_5 moved PreLogin logic to CraftBukkit
        CALL_PLAYER_PRE_LOGIN_EVENTS = getMethod(
                LOGIN_LISTENER,
                "callPlayerPreLoginEvents",
                GameProfile.class
        );
        IS_POST_LOGIN_HANDLER = CALL_PLAYER_PRE_LOGIN_EVENTS != null;

        if (IS_PRE_1_20_2) {
            Class<?> packetListenerClass = getClassOrFallback(
                    "net.minecraft.network.PacketListener",
                    nmsPackage + "PacketListener"
            );

            PACKET_LISTENER = getFieldOfType(networkManager, packetListenerClass);
        } else {
            // We get the field by name on 1.20.2+ as there are now multiple fields of this type in network manager

            // PacketListener packetListener of NetworkManager
            PACKET_LISTENER = getField(networkManager, "packetListener", "q");
            makeAccessible(PACKET_LISTENER);
        }
        checkNotNull(PACKET_LISTENER, "Packet listener");

        if (IS_POST_LOGIN_HANDLER) {
            makeAccessible(CALL_PLAYER_PRE_LOGIN_EVENTS);

            START_CLIENT_VERIFICATION = getMethod(LOGIN_LISTENER, "startClientVerification", "b", GameProfile.class);
            checkNotNull(START_CLIENT_VERIFICATION, "startClientVerification");
            makeAccessible(START_CLIENT_VERIFICATION);

            LOGIN_HANDLER_CONSTRUCTOR = null;
            FIRE_LOGIN_EVENTS = null;
            FIRE_LOGIN_EVENTS_GAME_PROFILE = null;
        } else {
            Class<?> loginHandler = getClassOrFallback(
                    "net.minecraft.server.network.LoginListener$LoginHandler",
                    nmsPackage + "LoginListener$LoginHandler"
            );

            LOGIN_HANDLER_CONSTRUCTOR =
                    ReflectionUtils.getConstructor(loginHandler, true, LOGIN_LISTENER);
            checkNotNull(LOGIN_HANDLER_CONSTRUCTOR, "LoginHandler constructor");

            FIRE_LOGIN_EVENTS = getMethod(loginHandler, "fireEvents");

            // LoginHandler().fireEvents(GameProfile)
            FIRE_LOGIN_EVENTS_GAME_PROFILE = getMethod(loginHandler, "fireEvents",
                    GameProfile.class);
            checkNotNull(FIRE_LOGIN_EVENTS, FIRE_LOGIN_EVENTS_GAME_PROFILE,
                    "fireEvents from LoginHandler", "fireEvents(GameProfile) from LoginHandler");

            START_CLIENT_VERIFICATION = null;
        }

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

        IS_FOLIA = ReflectionUtils.getClassSilently(
                "io.papermc.paper.threadedregions.RegionizedServer"
        ) != null;

        if (!IS_PRE_1_20_2) {
            // PacketHandshakingInSetProtocol is now a record
            // This means its fields are now private and final
            // We therefore must use reflection to obtain the constructor
            CLIENT_INTENT = getClassOrFallback(
                    "net.minecraft.network.protocol.handshake.ClientIntent",
                    nmsPackage + "ClientIntent"
            );
            checkNotNull(CLIENT_INTENT, "Client intent enum");

            HANDSHAKE_PACKET_CONSTRUCTOR = getConstructor(HANDSHAKE_PACKET, false, int.class,
                    String.class, int.class, CLIENT_INTENT);
            checkNotNull(HANDSHAKE_PACKET_CONSTRUCTOR, "Handshake packet constructor");

            Field a = getField(HANDSHAKE_PACKET, "STREAM_CODEC", "a");
            checkNotNull(a, "Handshake \"a\" field (protocol version, or stream codec)");

            if (a.getType().isPrimitive()) { // 1.20.2 - 1.20.4: a is the protocol version (int)
                HANDSHAKE_PROTOCOL = a;
                HANDSHAKE_PORT = getField(HANDSHAKE_PACKET, "c");
            } else { // 1.20.5: a is the stream_codec thing, so everything is shifted
                HANDSHAKE_PROTOCOL = getField(HANDSHAKE_PACKET, "protocolVersion", "b");
                HANDSHAKE_PORT = getField(HANDSHAKE_PACKET, "port", "d");
            }

            checkNotNull(HANDSHAKE_PROTOCOL, "Handshake protocol");
            makeAccessible(HANDSHAKE_PROTOCOL);

            checkNotNull(HANDSHAKE_PORT, "Handshake port");
            makeAccessible(HANDSHAKE_PORT);

            HANDSHAKE_INTENTION = getFieldOfType(HANDSHAKE_PACKET, CLIENT_INTENT);
            checkNotNull(HANDSHAKE_INTENTION, "Handshake intention");
            makeAccessible(HANDSHAKE_INTENTION);
        } else {
            CLIENT_INTENT = null;
            HANDSHAKE_PACKET_CONSTRUCTOR = null;
            HANDSHAKE_PORT = null;
            HANDSHAKE_PROTOCOL = null;
            HANDSHAKE_INTENTION = null;
        }
    }

    private static <T> T checkNotNull(@CheckForNull T toCheck, @CheckForNull String objectName) {
        return Preconditions.checkNotNull(toCheck, objectName + " cannot be null");
    }

    // Ensure one of two is not null
    private static <T> T checkNotNull(
            @CheckForNull T toCheck,
            @CheckForNull T toCheck2,
            @CheckForNull String objectName,
            @CheckForNull String objectName2
    ) {
        return Preconditions.checkNotNull(toCheck != null ? toCheck : toCheck2,
                objectName2 + " cannot be null if " + objectName + " is null");
    }
}
