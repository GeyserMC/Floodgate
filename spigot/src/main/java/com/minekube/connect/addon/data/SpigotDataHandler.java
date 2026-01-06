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

package com.minekube.connect.addon.data;

import static com.minekube.connect.util.ReflectionUtils.getCastedValue;
import static com.minekube.connect.util.ReflectionUtils.setValue;

import com.google.gson.Gson;
import com.minekube.connect.SpigotPlugin;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.network.netty.LocalSession.Context;
import com.minekube.connect.util.ClassNames;
import com.minekube.connect.util.ProxyUtils;
import com.mojang.authlib.GameProfile;
import java.net.InetSocketAddress;
import java.util.function.UnaryOperator;

public final class SpigotDataHandler extends CommonDataHandler {
    private final Context sessionCtx;
    private final String packetHandlerName;
    private final ConnectLogger logger;

    public SpigotDataHandler(
            Context sessionCtx,
            String packetHandlerName,
            ConnectConfig config,
            ConnectLogger logger) {
        super(config);
        this.sessionCtx = sessionCtx;
        this.packetHandlerName = packetHandlerName;
        this.logger = logger;
    }

    private void removeSelf() {
        ctx.pipeline().remove(this);
    }

    private void debug(String message) {
        if (config.isDebug()) {
            logger.info("[DataHandler] " + message);
        }
    }

    @Override
    public boolean channelRead(Object packet) throws Exception {
        if (ClassNames.HANDSHAKE_PACKET.isInstance(packet)) {
            if (ProxyUtils.isBungeeData()) {
                // Server has bungee enabled and expects to receive player data in hostname of handshake packet.
                // See https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse/CraftBukkit-Patches/0031-BungeeCord-Support.patch#70
                String hostname = getCastedValue(packet, ClassNames.HANDSHAKE_HOST);

                // Strip any pre-existing BungeeCord data from hostname (moxy may have added it)
                // BungeeCord data is separated by \0, so get just the virtual host part
                if (hostname.contains("\0")) {
                    hostname = hostname.split("\0")[0];
                    debug("Stripped pre-existing BungeeCord data from hostname");
                }

                String newHostname = createBungeeForwardingAddress(hostname);

                if (ClassNames.IS_PRE_1_20_2) {
                    // 1.20.1 and below - can modify final field directly
                    setValue(packet, ClassNames.HANDSHAKE_HOST, newHostname);
                } else {
                    // 1.20.2+ - final fields cannot be modified, create new packet instead
                    try {
                        Object[] components = new Object[]{
                                getCastedValue(packet, ClassNames.HANDSHAKE_PROTOCOL),
                                newHostname, // new hostname
                                getCastedValue(packet, ClassNames.HANDSHAKE_PORT),
                                getCastedValue(packet, ClassNames.HANDSHAKE_INTENTION)
                        };
                        Object newPacket = ClassNames.HANDSHAKE_PACKET_CONSTRUCTOR.newInstance(components);
                        // Replace the packet in the pipeline
                        ctx.fireChannelRead(newPacket);
                        if (!SpigotPlugin.isProtocolSupport()) {
                            removeSelf();
                        }
                        return false; // Don't pass the original packet
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to create new Handshake packet", e);
                    }
                }

                if (!SpigotPlugin.isProtocolSupport()) { // if we don't check this the player would be kicked with "unexpected hello packet"
                    removeSelf();
                }
                return true;
            } else {
                // BungeeCord mode is disabled, but moxy might have sent BungeeCord data anyway
                // Strip it to prevent Paper from rejecting with "Unknown data in login hostname"
                String hostname = getCastedValue(packet, ClassNames.HANDSHAKE_HOST);
                if (hostname.contains("\0")) {
                    hostname = hostname.split("\0")[0];
                    debug("BungeeCord mode disabled but hostname contained forwarding data, stripped it");

                    if (ClassNames.IS_PRE_1_20_2) {
                        setValue(packet, ClassNames.HANDSHAKE_HOST, hostname);
                    } else {
                        // 1.20.2+ - final fields cannot be modified, create new packet instead
                        try {
                            Object[] components = new Object[]{
                                    getCastedValue(packet, ClassNames.HANDSHAKE_PROTOCOL),
                                    hostname, // stripped hostname
                                    getCastedValue(packet, ClassNames.HANDSHAKE_PORT),
                                    getCastedValue(packet, ClassNames.HANDSHAKE_INTENTION)
                            };
                            Object newPacket = ClassNames.HANDSHAKE_PACKET_CONSTRUCTOR.newInstance(components);
                            ctx.fireChannelRead(newPacket);
                            if (!SpigotPlugin.isProtocolSupport()) {
                                removeSelf();
                            }
                            return false; // Don't pass the original packet
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to create new Handshake packet", e);
                        }
                    }
                }
            }
            // When BungeeCord mode is disabled, don't remove self - we need to handle LOGIN_START_PACKET
            return true; // next is LOGIN_START_PACKET
        }
        if (ClassNames.LOGIN_START_PACKET.isInstance(packet)) {
            debug("Processing LOGIN_START_PACKET for " + sessionCtx.getPlayer().getUsername());

            Object networkManager = ctx.channel().pipeline().get(packetHandlerName);
            Object packetListener = ClassNames.PACKET_LISTENER.get(networkManager);

            if (config.isDebug()) {
                debug("networkManager: " + (networkManager != null ? networkManager.getClass().getName() : "null"));
                debug("packetListener: " + (packetListener != null ? packetListener.getClass().getName() : "null"));
            }

            // Use spoofedUUID for initUUID (just like BungeeCord)
            // (Also we need to set spoofedUUID before our ProtocolSupport hack triggers PlayerProfileCompleteEvent
            // that does this https://github.com/ProtocolSupport/ProtocolSupport/blob/aed01898d769701d07a3a66ad55b8524d9937e55/src/protocolsupport/protocol/packet/handler/AbstractLoginListener.java#L192)
            setValue(networkManager, "spoofedUUID", sessionCtx.getPlayer().getUniqueId());

            // check if the server is actually in the Login state
            if (!ClassNames.LOGIN_LISTENER.isInstance(packetListener)) {
                // player is not in the login state, abort
                debug("ABORT: packetListener is not LOGIN_LISTENER, was: " +
                      (packetListener != null ? packetListener.getClass().getName() : "null"));

                // I would've liked to close the channel for security reasons, but our big friend
                // ProtocolSupport, who likes to break things, doesn't work otherwise
                removeSelf();
                return false;
            }

            if (ProxyUtils.isVelocitySupport()) {
                // We securely skip the velocity plugin message login method entirely.
                // Setting this field to a value other than -1 skips the velocity check in paper and
                // prevents the player getting kicked due to "This server requires you to connect with Velocity."
                // when invoking LoginHandler#fireEvents() below.
                setValue(packetListener, ClassNames.VELOCITY_LOGIN_MESSAGE_ID, 0);
            }

            // Set the player's correct GameProfile
            GameProfile gameProfile = new GameProfile(
                    sessionCtx.getPlayer().getUniqueId(),
                    sessionCtx.getPlayer().getUsername()
            );

            // We have to fake the offline player (login) cycle
            if (ClassNames.IS_PRE_1_20_2) {
                // 1.20.1 and below
                // - set profile, otherwise the username doesn't change
                // - LoginListener#initUUID
                // - new LoginHandler().fireEvents();
                // and the tick of LoginListener will do the rest
                debug("Using pre-1.20.2 login flow");

                Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
                setValue(packetListener, ClassNames.LOGIN_PROFILE, gameProfile);
                ClassNames.INIT_UUID.invoke(packetListener);
                ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);
            } else if (!ClassNames.IS_POST_LOGIN_HANDLER) {
                // 1.20.2 until somewhere in 1.20.4 we can directly register the profile
                debug("Using 1.20.2-1.20.4 login flow");

                Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
                ClassNames.FIRE_LOGIN_EVENTS_GAME_PROFILE.invoke(loginHandler, gameProfile);
            } else {
                // somewhere during 1.20.4 md_5 moved stuff to CraftBukkit
                debug("Using post-1.20.4 login flow (callPlayerPreLoginEvents + startClientVerification)");

                // LoginListener#callPlayerPreLoginEvents(GameProfile) returns modified GameProfile
                // LoginListener#startClientVerification(GameProfile)
                Object returnedProfile = ClassNames.CALL_PLAYER_PRE_LOGIN_EVENTS.invoke(packetListener, gameProfile);
                GameProfile profileToUse = returnedProfile != null ? (GameProfile) returnedProfile : gameProfile;

                if (config.isDebug()) {
                    debug("callPlayerPreLoginEvents returned profile: " + profileToUse);
                }

                ClassNames.START_CLIENT_VERIFICATION.invoke(packetListener, profileToUse);
                debug("startClientVerification completed");
            }

            removeSelf();
            return false;
        }
        return true;
    }

    private static final Gson GSON = new Gson();

    // source https://github.com/PaperMC/Velocity/blob/2586210ca67f2510eb4f91bf7567643f8a26ee7b/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/VelocityServerConnection.java#L126
    private String createBungeeForwardingAddress(String virtualHost) {
        // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
        // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
        // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
        StringBuilder data = new StringBuilder()
                .append(virtualHost)
                .append('\0')
                .append(getPlayerRemoteAddressAsString())
                .append('\0')
                .append(sessionCtx.getPlayer().getUniqueId().toString().replaceAll("-", ""))
                .append('\0');
        GSON.toJson(UnaryOperator.identity().apply(
                sessionCtx.getPlayer().getGameProfile().getProperties()), data);
        return data.toString();
    }

    private String getPlayerRemoteAddressAsString() {
        final String addr = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        int ipv6ScopeIdx = addr.indexOf('%');
        if (ipv6ScopeIdx == -1) {
            return addr;
        } else {
            return addr.substring(0, ipv6ScopeIdx);
        }
    }

}
