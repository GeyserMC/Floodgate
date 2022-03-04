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
import com.minekube.connect.config.FloodgateConfig;
import com.minekube.connect.network.netty.LocalSession.Context;
import com.minekube.connect.util.ClassNames;
import com.minekube.connect.util.ProxyUtils;
import com.mojang.authlib.GameProfile;
import java.net.InetSocketAddress;
import java.util.function.UnaryOperator;

public final class SpigotDataHandler extends CommonDataHandler {
    private final Context sessionCtx;
    private final String packetHandlerName;

    public SpigotDataHandler(
            Context sessionCtx,
            String packetHandlerName,
            FloodgateConfig config) {
        super(config);
        this.sessionCtx = sessionCtx;
        this.packetHandlerName = packetHandlerName;
    }

    private void removeSelf() {
        ctx.pipeline().remove(this);
    }

    @Override
    public boolean channelRead(Object packet) throws Exception {
        if (ClassNames.HANDSHAKE_PACKET.isInstance(packet)) {
            if (ProxyUtils.isBungeeData()) {
                // Server has bungee enabled and expects to receive player data in hostname of handshake packet.
                // See https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse/CraftBukkit-Patches/0031-BungeeCord-Support.patch#70
                // Let's modify the hostname, and we are done!
                String hostname = getCastedValue(packet, ClassNames.HANDSHAKE_HOST);
                setValue(packet,
                        ClassNames.HANDSHAKE_HOST,
                        createBungeeForwardingAddress(hostname));

                if (!SpigotPlugin.isProtocolSupport()) { // if we don't check this the player would be kicked with "unexpected hello packet"
                    removeSelf();
                }
                return true;
            }
            return true; // next is LOGIN_START_PACKET
        }
        if (ClassNames.LOGIN_START_PACKET.isInstance(packet)) {
            Object networkManager = ctx.channel().pipeline().get(packetHandlerName);
            Object packetListener = ClassNames.PACKET_LISTENER.get(networkManager);

            // Use spoofedUUID for initUUID (just like BungeeCord)
            // (Also we need to set spoofedUUID before our ProtocolSupport hack triggers PlayerProfileCompleteEvent
            // that does this https://github.com/ProtocolSupport/ProtocolSupport/blob/aed01898d769701d07a3a66ad55b8524d9937e55/src/protocolsupport/protocol/packet/handler/AbstractLoginListener.java#L192)
            setValue(networkManager, "spoofedUUID", sessionCtx.getPlayer().getUniqueId());

            // Seems that the skin is already showing without setting spoofedProfile (not sure why)
//            setValue(networkManager, "spoofedProfile", sessionCtx.getPlayer().getGameProfile()
//                    .getProperties().stream()
//                    .map(prop -> new Property(prop.getName(), prop.getValue(),
//                            prop.getSignature().isEmpty() ? null : prop.getSignature()))
//                    .toArray(Property[]::new)
//            );

            // check if the server is actually in the Login state
            if (!ClassNames.LOGIN_LISTENER.isInstance(packetListener)) {
                // player is not in the login state, abort

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
            setValue(packetListener, ClassNames.LOGIN_PROFILE, gameProfile);

            // We have to fake the offline player (login) cycle
            // just like on Spigot:

            // LoginListener#initUUID
            ClassNames.INIT_UUID.invoke(packetListener);
            // loginHandler = new LoginHandler()
            Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
            // loginHandler.fireEvents()
            ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);
            // and the tick of LoginListener will do the rest

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
