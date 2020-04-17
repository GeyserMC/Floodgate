package org.geysermc.floodgate;

import lombok.Getter;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.packet.Handshake;
import org.geysermc.floodgate.HandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.HandshakeHandler.ResultType;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;

public class BungeePlugin extends Plugin implements Listener {
    @Getter private static BungeePlugin instance;
    private static Field extraHandshakeData;

    @Getter private BungeeFloodgateConfig config;
    private BungeeDebugger debugger;
    private HandshakeHandler handshakeHandler;

    @Override
    public void onLoad() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        config = FloodgateConfig.load(getLogger(), getDataFolder().toPath().resolve("config.yml"), BungeeFloodgateConfig.class);
        handshakeHandler = new HandshakeHandler(config.getPrivateKey(), true, config.getUsernamePrefix(), config.isReplaceSpaces());
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        if (config.isDebug()) {
            debugger = new BungeeDebugger();
        }
    }

    @Override
    public void onDisable() {
        if (config.isDebug()) {
            getLogger().warning("Please note that it is not possible to reload this plugin when debug mode is enabled. At least for now");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnect(ServerConnectEvent e) {
        // Passes the information through to the connecting server if enabled
        if (config.isSendFloodgateData() && FloodgateAPI.isBedrockPlayer(e.getPlayer())) {
            Handshake handshake = ReflectionUtil.getCastedValue(e.getPlayer().getPendingConnection(), "handshake", Handshake.class);
            handshake.setHost(
                    handshake.getHost().split("\0")[0] + '\0' + // Ensures that only the hostname remains!
                            FLOODGATE_IDENTIFIER + '\0' + FloodgateAPI.getEncryptedData(e.getPlayer().getUniqueId())
            );
            // Bungeecord will add his data after our data
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(PreLoginEvent event) {
        event.registerIntent(this);
        getProxy().getScheduler().runAsync(this, () -> {
            String extraData = ReflectionUtil.getCastedValue(event.getConnection(), extraHandshakeData, String.class);

            HandshakeResult result = handshakeHandler.handle(extraData);
            switch (result.getResultType()) {
                case SUCCESS:
                    break;
                case EXCEPTION:
                    event.setCancelReason(config.getMessages().getInvalidKey());
                    break;
                case INVALID_DATA_LENGTH:
                    event.setCancelReason(String.format(
                            config.getMessages().getInvalidArgumentsLength(),
                            BedrockData.EXPECTED_LENGTH, result.getBedrockData().getDataLength()
                    ));
                    break;
            }

            if (result.getResultType() != ResultType.SUCCESS) {
                // only continue when SUCCESS
                event.completeIntent(this);
                return;
            }

            FloodgatePlayer player = result.getFloodgatePlayer();
            FloodgateAPI.addEncryptedData(player.getCorrectUniqueId(), result.getHandshakeData()[2] + '\0' + result.getHandshakeData()[3]);

            event.getConnection().setOnlineMode(false);
            event.getConnection().setUniqueId(player.getCorrectUniqueId());

            ReflectionUtil.setValue(event.getConnection(), "name", player.getJavaUsername());
            Object channelWrapper = ReflectionUtil.getValue(event.getConnection(), "ch");
            SocketAddress remoteAddress = ReflectionUtil.getCastedValue(channelWrapper, "remoteAddress", SocketAddress.class);
            if (!(remoteAddress instanceof InetSocketAddress)) {
                getLogger().info(
                        "Player " + player.getUsername() + " doesn't use an InetSocketAddress. " +
                        "It uses " + remoteAddress.getClass().getSimpleName() + ". Ignoring the player, I guess."
                );
            } else {
                ReflectionUtil.setValue(
                        channelWrapper, "remoteAddress",
                        new InetSocketAddress(result.getBedrockData().getIp(), ((InetSocketAddress) remoteAddress).getPort())
                );
            }
            event.completeIntent(this);
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        FloodgatePlayer player = FloodgateAPI.getPlayerByConnection(event.getPlayer().getPendingConnection());
        if (player != null) {
            FloodgateAPI.players.remove(player.getCorrectUniqueId());
            FloodgateAPI.removeEncryptedData(player.getCorrectUniqueId());
            System.out.println("Removed " + player.getUsername() + " " + event.getPlayer().getUniqueId());
        }
    }

    static {
        ReflectionUtil.setPrefix("net.md_5.bungee");
        Class<?> initial_handler = ReflectionUtil.getPrefixedClass("connection.InitialHandler");
        extraHandshakeData = ReflectionUtil.getField(initial_handler, "extraDataInHandshake");
    }
}
