package org.geysermc.floodgate.addon.data;

import com.google.common.collect.Queues;
import org.geysermc.floodgate.mixin.ClientConnectionMixin;
import org.geysermc.floodgate.mixin_interface.ServerLoginNetworkHandlerSetter;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.Constants;

import java.net.InetSocketAddress;
import java.util.Queue;

@RequiredArgsConstructor
public final class FabricDataHandler extends ChannelInboundHandlerAdapter {
    private final FloodgateConfig config;
    private final FloodgateHandshakeHandler handshakeHandler;
    private final PacketBlocker blocker;
    private final FloodgateLogger logger;

    private final Queue<Object> packetQueue = Queues.newConcurrentLinkedQueue();

    private ClientConnection networkManager;
    private FloodgatePlayer player;

    /**
     * As a variable so we can change it without reflection
     */
    HandshakeC2SPacket handshakePacket;
    /**
     * A boolean to compensate for the above
     */
    private boolean packetsBlocked;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) {
        // prevent other packets from being handled while we handle the handshake packet
        if (packetsBlocked) {
            packetQueue.add(packet);
            return;
        }

        if (packet instanceof HandshakeC2SPacket handshakePacket) {
            blocker.enable();
            packetsBlocked = true;
            this.handshakePacket = handshakePacket;

            networkManager = (ClientConnection) ctx.channel().pipeline().get("packet_handler");

            handshakeHandler.handle(ctx.channel(), handshakePacket.getAddress()).thenApply(result -> {
                HandshakeData handshakeData = result.getHandshakeData();

                this.handshakePacket = new HandshakeC2SPacket(handshakeData.getHostname(),
                        handshakePacket.getPort(), handshakePacket.getIntendedState());

                InetSocketAddress newIp = result.getNewIp(ctx.channel());
                if (newIp != null) {
                    ((ClientConnectionMixin) networkManager).setAddress(newIp);
                }

                if (handshakeData.getDisconnectReason() != null) {
                    ctx.close(); //todo disconnect with message
                    return true;
                }

                //todo use kickMessageAttribute and let this be common logic

                switch (result.getResultType()) {
                    case SUCCESS:
                        break;
                    case EXCEPTION:
                        logger.info(Constants.INTERNAL_ERROR_MESSAGE);
                        ctx.close();
                        return true;
                    case DECRYPT_ERROR:
                        logger.info(config.getDisconnect().getInvalidKey());
                        ctx.close();
                        return true;
                    case INVALID_DATA_LENGTH:
                        int dataLength = result.getBedrockData().getDataLength();
                        logger.info(
                                config.getDisconnect().getInvalidArgumentsLength(),
                                BedrockData.EXPECTED_LENGTH, dataLength
                        );
                        ctx.close();
                        return true;
                    default: // only continue when SUCCESS
                        return true;
                }

                player = result.getFloodgatePlayer();
                return player == null;
            }).thenAccept(shouldRemove -> {
                ctx.fireChannelRead(this.handshakePacket);
                Object queuedPacket;
                while ((queuedPacket = packetQueue.poll()) != null) {
                    if (checkLogin(ctx, packet)) {
                        break;
                    }
                    ctx.fireChannelRead(queuedPacket);
                }

                if (shouldRemove) {
                    ctx.pipeline().remove(FabricDataHandler.this);
                }
                blocker.disable();
                packetsBlocked = false;
            });
            return;
        }

        if (!checkLogin(ctx, packet)) {
            ctx.fireChannelRead(packet);
        }
    }

    private boolean checkLogin(ChannelHandlerContext ctx, Object packet) {
        if (packet instanceof LoginHelloC2SPacket) {
            // we have to fake the offline player (login) cycle
            if (!(networkManager.getPacketListener() instanceof ServerLoginNetworkHandler)) {
                // player is not in the login state, abort
                ctx.pipeline().remove(this);
                return true;
            }

            GameProfile gameProfile = new GameProfile(player.getCorrectUniqueId(), player.getCorrectUsername());

            ((ServerLoginNetworkHandlerSetter) networkManager.getPacketListener()).setGameProfile(gameProfile);
            ((ServerLoginNetworkHandlerSetter) networkManager.getPacketListener()).setLoginState();

            ctx.pipeline().remove(this);
            return true;
        }
        return false;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.isDebug()) {
            cause.printStackTrace();
        }
    }
}
