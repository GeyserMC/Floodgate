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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.ProxyUtils;

@RequiredArgsConstructor
public final class SpigotDataHandler extends ChannelInboundHandlerAdapter {
    private final FloodgateConfig config;
    private final FloodgateHandshakeHandler handshakeHandler;
    private final FloodgateLogger logger;
    private Object networkManager;
    private FloodgatePlayer player;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        boolean isHandshake = ClassNames.HANDSHAKE_PACKET.isInstance(packet);
        boolean isLogin = ClassNames.LOGIN_START_PACKET.isInstance(packet);

        boolean bungeeData = false;

        try {
            if (isHandshake) {
                networkManager = ctx.channel().pipeline().get("packet_handler");

                String handshakeValue = getCastedValue(packet, ClassNames.HANDSHAKE_HOST);
                HandshakeResult result = handshakeHandler.handle(ctx.channel(), handshakeValue);
                HandshakeData handshakeData = result.getHandshakeData();

                setValue(packet, ClassNames.HANDSHAKE_HOST, handshakeData.getHostname());

                InetSocketAddress newIp = result.getNewIp(ctx.channel());
                if (newIp != null) {
                    setValue(networkManager, ClassNames.SOCKET_ADDRESS, newIp);
                    //todo the socket address will be overridden when bungeeData is true
                }

                if (handshakeData.getDisconnectReason() != null) {
                    ctx.close(); //todo disconnect with message
                    return;
                }

                //todo use kickMessageAttribute and let this be common logic

                switch (result.getResultType()) {
                    case SUCCESS:
                        break;
                    case EXCEPTION:
                        logger.info(config.getDisconnect().getInvalidKey());
                        ctx.close();
                        return;
                    case INVALID_DATA_LENGTH:
                        int dataLength = result.getBedrockData().getDataLength();
                        logger.info(
                                config.getDisconnect().getInvalidArgumentsLength(),
                                BedrockData.EXPECTED_LENGTH, dataLength
                        );
                        ctx.close();
                        return;
                    case TIMESTAMP_DENIED:
                        logger.info(Constants.TIMESTAMP_DENIED_MESSAGE);
                        ctx.close();
                        return;
                    default: // only continue when SUCCESS
                        return;
                }

                player = result.getFloodgatePlayer();
                bungeeData = ProxyUtils.isProxyData();

                if (!bungeeData) {
                    // Use a spoofedUUID for initUUID (just like Bungeecord)
                    setValue(networkManager, "spoofedUUID", player.getCorrectUniqueId());
                }
            } else if (isLogin) {
                // we have to fake the offline player (login) cycle
                Object loginListener = ClassNames.PACKET_LISTENER.get(networkManager);

                // check if the server is actually in the Login state
                if (!ClassNames.LOGIN_LISTENER.isInstance(loginListener)) {
                    // player is not in the login state, abort
                    return;
                }

                // set the player his GameProfile, we can't change the username without this
                GameProfile gameProfile = new GameProfile(
                        player.getCorrectUniqueId(), player.getCorrectUsername()
                );
                setValue(loginListener, ClassNames.LOGIN_PROFILE, gameProfile);

                // just like on Spigot:

                // LoginListener#initUUID
                // new LoginHandler().fireEvents();

                // and the tick of LoginListener will do the rest

                ClassNames.INIT_UUID.invoke(loginListener);

                Object loginHandler =
                        ClassNames.LOGIN_HANDLER_CONSTRUCTOR.newInstance(loginListener);
                ClassNames.FIRE_LOGIN_EVENTS.invoke(loginHandler);
            }
        } finally {
            // don't let the packet through if the packet is the login packet
            if (!isLogin) {
                ctx.fireChannelRead(packet);
            }

            if (isHandshake && bungeeData || isLogin || player == null) {
                // We're done
                ctx.pipeline().remove(this);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.isDebug()) {
            cause.printStackTrace();
        }
    }
}
