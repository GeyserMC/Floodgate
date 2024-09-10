package org.geysermc.floodgate.mod.data;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.core.addon.data.CommonDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.mod.MinecraftServerHolder;
import org.geysermc.floodgate.mod.mixin.ClientIntentionPacketMixinInterface;
import org.geysermc.floodgate.mod.mixin.ConnectionMixin;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public final class ModDataHandler extends CommonDataHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final FloodgateLogger logger;
    private Connection networkManager;
    private FloodgatePlayer player;

    public ModDataHandler(
            FloodgateHandshakeHandler handshakeHandler,
            FloodgateConfig config,
            AttributeKey<String> kickMessageAttribute, FloodgateLogger logger) {
        super(handshakeHandler, config, kickMessageAttribute, new PacketBlocker());
        this.logger = logger;
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
        if (packet instanceof ClientIntentionPacket intentionPacket) {
            ctx.pipeline().addAfter("splitter", "floodgate_packet_blocker", blocker);
            networkManager = (Connection) ctx.channel().pipeline().get("packet_handler");
            handle(packet, intentionPacket.hostName());
            return false;
        }
        return !checkAndHandleLogin(packet);
    }

    private boolean checkAndHandleLogin(Object packet) {
        if (packet instanceof ServerboundHelloPacket) {
            String kickMessage = getKickMessage();
            if (kickMessage != null) {
                networkManager.disconnect(Component.nullToEmpty(kickMessage));
                return true;
            }

            // we have to fake the offline player (login) cycle
            if (!(networkManager.getPacketListener() instanceof ServerLoginPacketListenerImpl packetListener)) {
                // player is not in the login state, abort
                ctx.pipeline().remove(this);
                return true;
            }

            GameProfile gameProfile = new GameProfile(player.getCorrectUniqueId(), player.getCorrectUsername());

            if (player.isLinked() && player.getCorrectUniqueId().version() == 4) {
                verifyLinkedPlayerAsync(packetListener, gameProfile);
            } else {
                packetListener.startClientVerification(gameProfile);
            }

            ctx.pipeline().remove(this);
            return true;
        }
        return false;
    }

    /**
     * Starts a new thread that fetches the linked player's textures,
     * and then starts client verification with the more accurate game profile.
     *
     * @param packetListener the login packet listener for this connection
     * @param gameProfile the player's initial profile. it will NOT be mutated.
     */
    private void verifyLinkedPlayerAsync(ServerLoginPacketListenerImpl packetListener, GameProfile gameProfile) {
        Thread texturesThread = new Thread("Bedrock Linked Player Texture Download") {
            @Override
            public void run() {
                GameProfile effectiveProfile = gameProfile;
                try {
                    MinecraftSessionService service = MinecraftServerHolder.get().getSessionService();
                    effectiveProfile = service.fetchProfile(effectiveProfile.getId(), true).profile();
                } catch (Exception e) {
                    LOGGER.error("Unable to get Bedrock linked player textures for " + effectiveProfile.getName(), e);
                }
                packetListener.startClientVerification(effectiveProfile);
            }
        };
        texturesThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        texturesThread.start();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.isDebug()) {
            LOGGER.error("Exception caught in FabricDataHandler", cause);
        }
    }
}
