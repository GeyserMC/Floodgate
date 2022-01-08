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

package org.geysermc.floodgate.addon.data;

import static org.geysermc.floodgate.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.ProxyUtils;

public final class SpigotDataHandler extends CommonDataHandler {
    private Object networkManager;
    private FloodgatePlayer player;

    public SpigotDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            FloodgateConfig config,
            AttributeKey<String> kickMessageAttribute) {
        super(handshakeHandler, config, kickMessageAttribute, new PacketBlocker());
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        //todo the socket address will be overridden when bungeeData is true
        setValue(networkManager, ClassNames.SOCKET_ADDRESS, newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) {
        setValue(handshakePacket, ClassNames.HANDSHAKE_HOST, hostname);
        return handshakePacket;
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
        boolean needsAssistance = !ProxyUtils.isProxyData();
        
        // Paper and forks now have username validation, so we have to help Paper as well.
        // The username is only validated in the login start packet, and that packet doesn't reach
        // the server handler when we follow the non-bungee-data route
        needsAssistance |= ProxyUtils.isPaperServer();

        if (needsAssistance) {
            // Use a spoofedUUID for initUUID (just like Bungeecord)
            setValue(networkManager, "spoofedUUID", player.getCorrectUniqueId());
        }

        return !needsAssistance;
    }

    @Override
    protected boolean shouldCallFireRead(Object queuedPacket) {
        // we have to ignore the 'login start' packet if BungeeCord mode is disabled,
        // otherwise the server might ask the user to login
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

            // set the player his GameProfile, we can't change the username without this
            GameProfile gameProfile = new GameProfile(
                    player.getCorrectUniqueId(), player.getCorrectUsername()
            );
            setValue(packetListener, ClassNames.LOGIN_PROFILE, gameProfile);

            // we have to fake the offline player (login) cycle
            // just like on Spigot:

            // LoginListener#initUUID
            // new LoginHandler().fireEvents();

            // and the tick of LoginListener will do the rest

            ClassNames.INIT_UUID.invoke(packetListener);

            Object loginHandler =
                    ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(packetListener);
            ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);

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
