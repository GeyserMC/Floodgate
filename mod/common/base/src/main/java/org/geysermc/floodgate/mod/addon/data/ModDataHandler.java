package org.geysermc.floodgate.mod.addon.data;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.addon.data.CommonNettyDataHandler;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.mod.mixin.ClientIntentionPacketMixinInterface;
import org.geysermc.floodgate.mod.mixin.ConnectionMixin;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class ModDataHandler extends CommonNettyDataHandler {

    @Inject
    FloodgateLogger logger;

    private net.minecraft.network.Connection networkManager;

    @Inject
    @Named("minecraftServer")
    MinecraftServer minecraftServer;

    private Connection player;
    public ModDataHandler(
            DataSeeker dataSeeker,
            FloodgateDataHandler handshakeHandler,
            FloodgateConfig config,
            FloodgateLogger logger,
            AttributeKey<Connection> connectionAttribute,
            AttributeKey<String> kickMessageAttribute) {
        super(
                dataSeeker,
                handshakeHandler,
                config,
                logger,
                connectionAttribute,
                kickMessageAttribute,
                new PacketBlocker());
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
    protected boolean shouldRemoveHandler(FloodgateDataHandler.HandleResult result) {
        player = result.joinResult().connection();

        if (getKickMessage() != null) {
            // we also have to keep this handler if we want to kick then with a disconnect message
            return false;
        } else if (player == null) {
            // player is not a Floodgate player
            return true;
        }

        if (!result.joinResult().shouldDisconnect()) {
            logger.info("Floodgate player who is logged in as {} {} joined",
                    player.javaUsername(), player.javaUuid());
        }

        // Handler will be removed after the login hello packet is handled
        return false;
    }

    @Override
    protected boolean channelRead(Object packet) throws Exception {
        if (packet instanceof ClientIntentionPacket intentionPacket) {
            ctx.pipeline().addAfter("splitter", "floodgate_packet_blocker", blocker);
            networkManager = (net.minecraft.network.Connection) ctx.channel().pipeline().get("packet_handler");
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

            GameProfile gameProfile = new GameProfile(player.javaUuid(), player.javaUsername());

            if (player.isLinked() && player.javaUuid().version() == 4) {
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
                    MinecraftSessionService service = minecraftServer.getSessionService();
                    effectiveProfile = service.fetchProfile(effectiveProfile.getId(), true).profile();
                } catch (Exception e) {
                    logger.error("Unable to get Bedrock linked player textures for " + effectiveProfile.getName(), e);
                }
                packetListener.startClientVerification(effectiveProfile);
            }
        };
        texturesThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler((Logger) logger));
        texturesThread.start();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (config.debug()) {
            cause.printStackTrace();
        }
    }
}
