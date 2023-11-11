/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.fabric.addon.data;

import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.geysermc.floodgate.core.addon.data.CommonDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.fabric.MinecraftServerHolder;
import org.geysermc.floodgate.fabric.mixin.ConnectionMixin;
import org.geysermc.floodgate.fabric.mixin_interface.ClientIntentionPacketMixinInterface;
import org.geysermc.floodgate.fabric.mixin_interface.ServerLoginPacketListenerSetter;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public final class FabricDataHandler extends CommonDataHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Connection networkManager;
    private org.geysermc.api.connection.Connection player;

    public FabricDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            FloodgateConfig config,
            AttributeKey<String> kickMessageAttribute) {
        super(handshakeHandler, config, kickMessageAttribute, new PacketBlocker());
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        ((ConnectionMixin) this.networkManager).setAddress(newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) {
        // While it would be ideal to simply create a new handshake packet, the packet constructor
        // does not allow us to set the protocol version
        ((ClientIntentionPacketMixinInterface) handshakePacket).setAddress(hostname);
        return handshakePacket;
    }

    @Override
    protected boolean shouldRemoveHandler(FloodgateHandshakeHandler.HandshakeResult result) {
        player = result.getFloodgatePlayer();

        if (getKickMessage() != null) {
            // we also have to keep this handler if we want to kick then with a disconnect message
            return false;
        } else if (player == null) {
            // player is not a Floodgate player
            return true;
        }

        // Handler will be removed after the login hello packet is handled
        return false;
    }

    @Override
    protected boolean channelRead(Object packet) throws Exception {
        if (packet instanceof ClientIntentionPacket intentionPacket) {
            ctx.pipeline().addAfter("splitter", "floodgate_packet_blocker", blocker);
            networkManager = (Connection) ctx.channel().pipeline().get("packet_handler");
            handle(packet, intentionPacket.getHostName());
            return false;
        }
        return !checkAndHandleLogin(packet);
    }

    private boolean checkAndHandleLogin(Object packet) throws Exception {
        if (packet instanceof ServerboundHelloPacket) {
            String kickMessage = getKickMessage();
            if (kickMessage != null) {
                networkManager.disconnect(Component.nullToEmpty(kickMessage));
                return true;
            }

            // we have to fake the offline player (login) cycle
            if (!(networkManager.getPacketListener() instanceof ServerLoginPacketListenerImpl)) {
                // player is not in the login state, abort
                ctx.pipeline().remove(this);
                return true;
            }

            GameProfile gameProfile = new GameProfile(player.javaUuid(), player.javaUsername());

            if (player.isLinked() && player.javaUuid().version() == 4) {
                Thread texturesThread = new Thread("Bedrock Linked Player Texture Download") {
                    @Override
                    public void run() {
                        try {
                            MinecraftServerHolder.get().getSessionService()
                                    .fillProfileProperties(gameProfile, true);
                        } catch (Exception e) {
                            LOGGER.error("Unable to get Bedrock linked player textures for " + gameProfile.getName(), e);
                        }
                        ((ServerLoginPacketListenerSetter) networkManager.getPacketListener())
                                .setGameProfile(gameProfile);
                        ((ServerLoginPacketListenerSetter) networkManager.getPacketListener()).setLoginState();
                    }
                };
                texturesThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
                texturesThread.start();
            } else {
                ((ServerLoginPacketListenerSetter) networkManager.getPacketListener()).setGameProfile(gameProfile);
                ((ServerLoginPacketListenerSetter) networkManager.getPacketListener()).setLoginState();
            }

            ctx.pipeline().remove(this);
            return true;
        }
        return false;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.debug()) {
            cause.printStackTrace();
        }
    }
}
