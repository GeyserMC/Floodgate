package org.geysermc.floodgate.addon.data;

import org.geysermc.floodgate.mixin_interface.HandshakeS2CPacketAddressGetter;
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

@RequiredArgsConstructor
public class FabricDataHandler extends ChannelInboundHandlerAdapter {
    private final FloodgateConfig config;
    private final FloodgateHandshakeHandler handshakeHandler;
    private final FloodgateLogger logger;
    private ClientConnection networkManager;
    private FloodgatePlayer player;
    private boolean done;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        ReferenceCountUtil.retain(packet);
        if (done) {
            super.channelRead(ctx, packet);
            return;
        }

        boolean isHandshake = packet instanceof HandshakeC2SPacket;
        boolean isLogin = packet instanceof LoginHelloC2SPacket;
        try {
            if (isHandshake) {
                networkManager = (ClientConnection) ctx.channel().pipeline().get("packet_handler");

                String handshakeValue = ((HandshakeS2CPacketAddressGetter) packet).getAddress();
                FloodgateHandshakeHandler.HandshakeResult result = handshakeHandler.handle(ctx.channel(), handshakeValue);
                HandshakeData handshakeData = result.getHandshakeData();

                ((HandshakeS2CPacketAddressGetter) packet).setAddress(handshakeData.getHostname());

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
            } else if (isLogin) {
                // we have to fake the offline player (login) cycle
                if (!(networkManager.getPacketListener() instanceof ServerLoginNetworkHandler)) {
                    // player is not in the login state, abort
                    return;
                }

                GameProfile gameProfile = new GameProfile(player.getCorrectUniqueId(), player.getCorrectUsername());

                ((ServerLoginNetworkHandlerSetter) networkManager.getPacketListener()).setGameProfile(gameProfile);
                ((ServerLoginNetworkHandlerSetter) networkManager.getPacketListener()).setLoginState();
            }
        } finally {
            // don't let the packet through if the packet is the login packet
            // because we want to skip the login cycle
            if (isLogin) {
                ReferenceCountUtil.release(packet, 2);
            } else {
                ctx.fireChannelRead(packet);
            }

            if (isLogin || player == null) {
                // we're done, we'll just wait for the loginSuccessCall
                done = true;
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
