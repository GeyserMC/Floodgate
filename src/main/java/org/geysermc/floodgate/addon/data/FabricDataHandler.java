package org.geysermc.floodgate.addon.data;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.text.Text;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.mixin.ClientConnectionMixin;
import org.geysermc.floodgate.mixin.ClientIntentionPacketMixin;
import org.geysermc.floodgate.mixin_interface.ServerLoginNetworkHandlerSetter;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler.HandshakeResult;

import java.net.InetSocketAddress;

public final class FabricDataHandler extends CommonDataHandler {
    private final FloodgateLogger logger;
    private ClientConnection networkManager;
    private FloodgatePlayer player;

    public FabricDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            FloodgateConfig config,
            AttributeKey<String> kickMessageAttribute, FloodgateLogger logger) {
        super(handshakeHandler, config, kickMessageAttribute, new PacketBlocker());
        this.logger = logger;
    }

    @Override
    protected void setNewIp(Channel channel, InetSocketAddress newIp) {
        ((ClientConnectionMixin) this.networkManager).setAddress(newIp);
    }

    @Override
    protected Object setHostname(Object handshakePacket, String hostname) {
        // While it would be ideal to simply create a new handshake packet, the packet constructor
        // does not allow us to set the protocol version
        ((ClientIntentionPacketMixin) handshakePacket).setAddress(hostname);
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

        if (result.getResultType() == FloodgateHandshakeHandler.ResultType.SUCCESS) {
            logger.info("Floodgate player who is logged in as {} {} joined",
                    player.getCorrectUsername(), player.getCorrectUniqueId());
        }

        // Handler will be removed after the login hello packet is handled
        return false;
    }

    @Override
    protected boolean channelRead(Object packet) {
        if (packet instanceof HandshakeC2SPacket handshakePacket) {
            ctx.pipeline().addAfter("splitter", "floodgate_packet_blocker", blocker);
            networkManager = (ClientConnection) ctx.channel().pipeline().get("packet_handler");
            handle(packet, handshakePacket.getAddress());
            return false;
        }
        return !checkAndHandleLogin(packet);
    }

    private boolean checkAndHandleLogin(Object packet) {
        if (packet instanceof LoginHelloC2SPacket) {
            String kickMessage = getKickMessage();
            if (kickMessage != null) {
                networkManager.disconnect(Text.of(kickMessage));
                return true;
            }

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
