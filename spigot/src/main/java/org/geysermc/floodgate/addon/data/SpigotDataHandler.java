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

package org.geysermc.floodgate.addon.data;

import static org.geysermc.floodgate.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.ProxyUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

public final class SpigotDataHandler extends CommonDataHandler {
    private static final Property DEFAULT_TEXTURE_PROPERTY = new Property(
            "textures",
            Constants.DEFAULT_MINECRAFT_JAVA_SKIN_TEXTURE,
            Constants.DEFAULT_MINECRAFT_JAVA_SKIN_SIGNATURE
    );

    private final SpigotVersionSpecificMethods versionSpecificMethods;
    private Object networkManager;
    private FloodgatePlayer player;
    private boolean proxyData;

    public SpigotDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            FloodgateConfig config,
            AttributeKey<String> kickMessageAttribute,
            SpigotVersionSpecificMethods versionSpecificMethods
    ) {
        super(handshakeHandler, config, kickMessageAttribute, new PacketBlocker());
        this.versionSpecificMethods = versionSpecificMethods;
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        setValue(networkManager, ClassNames.SOCKET_ADDRESS, newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) throws IllegalStateException {
        if (ClassNames.IS_PRE_1_20_2) {
            // 1.20.1 and below
            setValue(handshakePacket, ClassNames.HANDSHAKE_HOST, hostname);

            return handshakePacket;
        } else {
            // 1.20.2 and above
            try {
                Object[] components = new Object[]{
                    ClassNames.HANDSHAKE_PROTOCOL.get(handshakePacket),
                    hostname,
                    ClassNames.HANDSHAKE_PORT.get(handshakePacket),
                    ClassNames.HANDSHAKE_INTENTION.get(handshakePacket)
                };

                return ClassNames.HANDSHAKE_PACKET_CONSTRUCTOR.newInstance(components);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to create new Handshake packet", e);
            }
        }
    }

    @Override
    protected boolean shouldRemoveHandler(HandshakeResult result) {
        player = result.getFloodgatePlayer();

        if (getKickMessage() != null) {
            // we also have to keep this handler if we want to kick then with a disconnect message
            return false;
        } else if (player == null) {
            // player is not a Floodgate player
            return true;
        }

        // the server will do all the work if BungeeCord mode is enabled,
        // otherwise we have to help the server.
        proxyData = ProxyUtils.isProxyData();

        if (!proxyData) {
            // Use a spoofedUUID for initUUID (just like Bungeecord)
            setValue(networkManager, "spoofedUUID", player.getCorrectUniqueId());
        }

        // we can only remove the handler if the data is proxy data and username validation doesn't
        // exist. Otherwise, we need to wait and disable it.
        return proxyData && ClassNames.PAPER_DISABLE_USERNAME_VALIDATION == null;
    }

    @Override
    protected boolean shouldCallFireRead(Object queuedPacket) {
        // we have to ignore the 'login start' packet,
        // otherwise the server will ask the user to login
        try {
            if (checkAndHandleLogin(queuedPacket)) {
                return false;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean channelRead(Object packet) throws Exception {
        if (ClassNames.HANDSHAKE_PACKET.isInstance(packet)) {
            // ProtocolSupport would break if we added this during the creation of this handler
            ctx.pipeline().addAfter("splitter", "floodgate_packet_blocker", blocker);

            networkManager = ctx.channel().pipeline().get("packet_handler");

            handle(packet, getCastedValue(packet, ClassNames.HANDSHAKE_HOST));
            // otherwise, it'll get read twice. once by the packet queue and once by this method
            return false;
        }

        return !checkAndHandleLogin(packet);
    }

    private boolean checkAndHandleLogin(Object packet) throws Exception {
        if (ClassNames.LOGIN_START_PACKET.isInstance(packet)) {
            Object packetListener = ClassNames.PACKET_LISTENER.get(networkManager);

            String kickMessage = getKickMessage();
            if (kickMessage != null) {
                disconnect(packetListener, kickMessage);
                return true;
            }

            // check if the server is actually in the Login state
            if (!ClassNames.LOGIN_LISTENER.isInstance(packetListener)) {
                // player is not in the login state, abort

                // I would've liked to close the channel for security reasons, but our big friend
                // ProtocolSupport, who likes to break things, doesn't work otherwise
                ctx.pipeline().remove(this);
                return true;
            }

            if (ClassNames.PAPER_DISABLE_USERNAME_VALIDATION != null) {
                // ensure that Paper will not be checking
                setValue(packetListener, ClassNames.PAPER_DISABLE_USERNAME_VALIDATION, true);
                if (proxyData) {
                    // the server will handle the rest if we have proxy data
                    ctx.pipeline().remove(this);
                    return false;
                }
            }

            // Apply a default texture even for linked players (where we'd have to look up the skin ourselves)
            // because it's a blocking operation. We'll just override the skin later
            GameProfile gameProfile = versionSpecificMethods.createGameProfile(
                    player.getCorrectUniqueId(),
                    player.getCorrectUsername(),
                    DEFAULT_TEXTURE_PROPERTY
            );

            // we have to fake the offline player (login) cycle

            if (ClassNames.IS_PRE_1_20_2) {
                // 1.20.1 and below
                // - set profile, otherwise the username doesn't change
                // - LoginListener#initUUID
                // - new LoginHandler().fireEvents();
                // and the tick of LoginListener will do the rest

                Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
                setValue(packetListener, ClassNames.LOGIN_PROFILE, gameProfile);
                ClassNames.INIT_UUID.invoke(packetListener);
                ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);
            } else if (!ClassNames.IS_POST_LOGIN_HANDLER) {
                // 1.20.2 until somewhere in 1.20.4 we can directly register the profile

                Object loginHandler = ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
                ClassNames.FIRE_LOGIN_EVENTS_GAME_PROFILE.invoke(loginHandler, gameProfile);
            } else {
                // somewhere during 1.20.4 md_5 moved stuff to CraftBukkit

                // LoginListener#callPlayerPreLoginEvents(GameProfile)
                // LoginListener#startClientVerification(GameProfile)
                ClassNames.CALL_PLAYER_PRE_LOGIN_EVENTS.invoke(packetListener, gameProfile);
                ClassNames.START_CLIENT_VERIFICATION.invoke(packetListener, gameProfile);
            }

            ctx.pipeline().remove(this);
            return true;
        }
        return false;
    }

    private void disconnect(Object packetListener, String kickMessage) throws Exception {
        // both versions close the channel for us
        if (ClassNames.LOGIN_LISTENER.isInstance(packetListener)) {
            ClassNames.LOGIN_DISCONNECT.invoke(packetListener, kickMessage);
        } else {
            // ProtocolSupport for example has their own PacketLoginInListener implementation
            ClassNames.NETWORK_EXCEPTION_CAUGHT.invoke(
                    networkManager,
                    ctx, new IllegalStateException(kickMessage)
            );
        }
    }
}
