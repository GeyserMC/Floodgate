package org.geysermc.floodgate.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.FloodgatePlayerImpl;
import org.geysermc.floodgate.HandshakeHandler;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.handler.BungeeDataHandler;

import java.util.UUID;

public final class BungeeListener implements Listener {
    private final BungeeDataHandler dataHandler;
    private final ProxyFloodgateApi api;
    private final FloodgateLogger logger;

    public BungeeListener(Plugin plugin, ProxyFloodgateConfig config, ProxyFloodgateApi api,
                          HandshakeHandler handshakeHandler, FloodgateLogger logger) {
        this.dataHandler = new BungeeDataHandler(plugin, config, api, handshakeHandler, logger);
        this.api = api;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnect(ServerConnectEvent event) {
        dataHandler.handle(event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(PreLoginEvent event) {
        dataHandler.handle(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLoginMonitor(PreLoginEvent event) {
        if (event.isCancelled()) {
            api.removePlayer(event.getConnection().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        // if there was another player with the same uuid / name online,
        // he has been disconnected by now
        UUID uniqueId = event.getConnection().getUniqueId();
        FloodgatePlayer player = api.getPlayer(uniqueId);
        logger.info("Login" + (player != null) + " " + uniqueId);
        if (player != null) {
            player.as(FloodgatePlayerImpl.class).setLogin(false);
            logger.info("Floodgate player who is logged in as {} {} joined",
                    player.getCorrectUsername(), player.getCorrectUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginMonitor(LoginEvent event) {
        logger.info("LoginMonitor" + event.isCancelled());
        if (event.isCancelled()) {
            api.removePlayer(event.getConnection().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        FloodgatePlayer fPlayer;
        if ((fPlayer = api.removePlayer(player.getUniqueId())) != null) {
            api.removeEncryptedData(player.getUniqueId());
            logger.info(
                    "Floodgate player who was logged in as {} {} disconnected",
                    player.getName(), player.getUniqueId()
            );
        }
        logger.info("Disconnected " + fPlayer);
    }
}
