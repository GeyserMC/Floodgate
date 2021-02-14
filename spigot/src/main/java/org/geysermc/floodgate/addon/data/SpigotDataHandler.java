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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.ReflectionUtils.getCastedValue;
import static org.geysermc.floodgate.util.ReflectionUtils.getFieldOfType;
import static org.geysermc.floodgate.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.util.ReflectionUtils.makeAccessible;
import static org.geysermc.floodgate.util.ReflectionUtils.setValue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotUtils;

@RequiredArgsConstructor
public final class SpigotDataHandler extends ChannelInboundHandlerAdapter {
    private static final Field SOCKET_ADDRESS;
    private static final Class<?> HANDSHAKE_PACKET;
    private static final Field HANDSHAKE_HOST;

    private static final Class<?> GAME_PROFILE;
    private static final Constructor<?> GAME_PROFILE_CONSTRUCTOR;
    private static final Field LOGIN_PROFILE;

    private static final Class<?> LOGIN_START_PACKET;
    private static final Class<?> LOGIN_LISTENER;
    private static final Method INIT_UUID;

    private static final Class<?> LOGIN_HANDLER;
    private static final Constructor<?> LOGIN_HANDLER_CONSTRUCTOR;
    private static final Method FIRE_LOGIN_EVENTS;

    private static final Field PACKET_LISTENER;
    private static final Field PROTOCOL_STATE;
    private static final Object READY_TO_ACCEPT_PROTOCOL_STATE;

    static {
        Class<?> networkManager = getPrefixedClass("NetworkManager");
        checkNotNull(networkManager, "NetworkManager class cannot be null");

        SOCKET_ADDRESS = getFieldOfType(networkManager, SocketAddress.class, false);
        checkNotNull(SOCKET_ADDRESS, "SocketAddress field cannot be null");

        HANDSHAKE_PACKET = getPrefixedClass("PacketHandshakingInSetProtocol");
        checkNotNull(HANDSHAKE_PACKET, "PacketHandshakingInSetProtocol cannot be null");
        HANDSHAKE_HOST = getFieldOfType(HANDSHAKE_PACKET, String.class);
        checkNotNull(HANDSHAKE_HOST, "Host field from handshake packet cannot be null");

        LOGIN_START_PACKET = getPrefixedClass("PacketLoginInStart");
        checkNotNull(LOGIN_START_PACKET, "PacketLoginInStart cannot be null");

        GAME_PROFILE = ReflectionUtils.getClass("com.mojang.authlib.GameProfile");
        checkNotNull(GAME_PROFILE, "GameProfile class cannot be null");

        Constructor<?> gameProfileConstructor = null;
        try {
            gameProfileConstructor = GAME_PROFILE.getConstructor(UUID.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        GAME_PROFILE_CONSTRUCTOR = gameProfileConstructor;
        checkNotNull(GAME_PROFILE_CONSTRUCTOR, "GameProfileConstructor cannot be null");

        LOGIN_LISTENER = getPrefixedClass("LoginListener");
        checkNotNull(LOGIN_LISTENER, "LoginListener cannot be null");
        LOGIN_PROFILE = getFieldOfType(LOGIN_LISTENER, GAME_PROFILE);
        checkNotNull(LOGIN_PROFILE, "Profile from LoginListener cannot be null");
        INIT_UUID = getMethod(LOGIN_LISTENER, "initUUID");
        checkNotNull(INIT_UUID, "initUUID from LoginListener cannot be null");

        Field protocolStateField = null;
        for (Field field : LOGIN_LISTENER.getDeclaredFields()) {
            if (field.getType().isEnum()) {
                protocolStateField = field;
            }
        }
        PROTOCOL_STATE = protocolStateField;
        checkNotNull(PROTOCOL_STATE, "Protocol state field from LoginListener cannot be null");

        Enum<?>[] protocolStates = (Enum<?>[]) PROTOCOL_STATE.getType().getEnumConstants();
        Object readyToAcceptState = null;
        for (Enum<?> protocolState : protocolStates) {
            if ("READY_TO_ACCEPT".equals(protocolState.name())) {
                readyToAcceptState = protocolState;
            }
        }
        READY_TO_ACCEPT_PROTOCOL_STATE = readyToAcceptState;
        checkNotNull(READY_TO_ACCEPT_PROTOCOL_STATE,
                "Ready to accept state from Protocol state cannot be null");

        Class<?> packetListenerClass = getPrefixedClass("PacketListener");
        PACKET_LISTENER = getFieldOfType(networkManager, packetListenerClass);
        checkNotNull(PACKET_LISTENER, "PacketListener cannot be null");

        LOGIN_HANDLER = getPrefixedClass("LoginListener$LoginHandler");
        checkNotNull(LOGIN_HANDLER, "LoginHandler cannot be null");

        Constructor<?> loginHandlerConstructor = null;
        try {
            loginHandlerConstructor = makeAccessible(
                    LOGIN_HANDLER.getDeclaredConstructor(LOGIN_LISTENER));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        LOGIN_HANDLER_CONSTRUCTOR = loginHandlerConstructor;
        checkNotNull(LOGIN_HANDLER_CONSTRUCTOR, "LoginHandler constructor cannot be null");

        FIRE_LOGIN_EVENTS = getMethod(LOGIN_HANDLER, "fireEvents");
        checkNotNull(FIRE_LOGIN_EVENTS, "fireEvents from LoginHandler cannot be null");
    }

    /* per player stuff */
    private final FloodgateConfig config;
    private final FloodgateHandshakeHandler handshakeHandler;
    private final FloodgateLogger logger;
    private Object networkManager;
    private FloodgatePlayer player;
    private boolean bungeeData;
    private boolean done;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        ReferenceCountUtil.retain(packet);
        // we're done but we're not yet removed from the connection
        if (done) {
            ctx.fireChannelRead(packet);
            return;
        }

        boolean isHandshake = HANDSHAKE_PACKET.isInstance(packet);
        boolean isLogin = LOGIN_START_PACKET.isInstance(packet);

        try {
            if (isHandshake) {
                networkManager = ctx.channel().pipeline().get("packet_handler");

                String handshakeValue = getCastedValue(packet, HANDSHAKE_HOST);
                HandshakeResult result = handshakeHandler.handle(ctx.channel(), handshakeValue);
                HandshakeData handshakeData = result.getHandshakeData();

                setValue(packet, HANDSHAKE_HOST, handshakeData.getHostname());
                logger.info(handshakeData.getHostname());

                if (handshakeData.getDisconnectReason() != null) {
                    ctx.close(); // todo disconnect with message
                    return;
                }

                switch (result.getResultType()) {
                    case SUCCESS:
                        break;
                    case INVALID_DATA_LENGTH:
                        int dataLength = result.getBedrockData().getDataLength();
                        logger.info(
                                config.getDisconnect().getInvalidArgumentsLength(),
                                BedrockData.EXPECTED_LENGTH, dataLength
                        );
                        ctx.close();
                        return;
                    default: // only continue when SUCCESS
                        return;
                }

                player = result.getFloodgatePlayer();
                bungeeData = SpigotUtils.isBungeeData();

                if (!bungeeData) {
                    // Use a spoofedUUID for initUUID (just like Bungeecord)
                    setValue(networkManager, "spoofedUUID", player.getCorrectUniqueId());

                    // Use the player his IP for stuff instead of Geyser his IP
                    InetSocketAddress address = player.getProperty(PropertyKey.SOCKET_ADDRESS);
                    setValue(networkManager, SOCKET_ADDRESS, address);
                }
            } else if (isLogin) {
                if (!bungeeData) {
                    // we have to fake the offline player (login) cycle
                    Object loginListener = PACKET_LISTENER.get(networkManager);

                    // check if the server is actually in the Login state
                    if (!LOGIN_LISTENER.isInstance(loginListener)) {
                        // player is not in the login state, abort
                        return;
                    }

                    // set the player his GameProfile, we can't change the username without this
                    Object gameProfile = GAME_PROFILE_CONSTRUCTOR.newInstance(
                            player.getCorrectUniqueId(), player.getCorrectUsername()
                    );
                    setValue(loginListener, LOGIN_PROFILE, gameProfile);

                    // just like on Spigot:

                    // LoginListener#initUUID
                    // new LoginHandler().fireEvents();
                    // LoginListener#protocolState = READY_TO_ACCEPT

                    // and the tick of LoginListener will do the rest

                    INIT_UUID.invoke(loginListener);
                    FIRE_LOGIN_EVENTS.invoke(LOGIN_HANDLER_CONSTRUCTOR.newInstance(loginListener));
                    setValue(loginListener, PROTOCOL_STATE, READY_TO_ACCEPT_PROTOCOL_STATE);
                }
            }
        } finally {
            // don't let the packet through if the packet is the login packet
            // because we want to skip the login cycle
            if (isLogin) {
                ReferenceCountUtil.release(packet, 2);
            } else {
                ctx.fireChannelRead(packet);
            }

            if (isHandshake && bungeeData || isLogin && !bungeeData || player == null) {
                // we're done, we'll just wait for the loginSuccessCall
                done = true;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }
}
