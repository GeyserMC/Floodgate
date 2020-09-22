package org.geysermc.floodgate.listener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.FloodgatePlayerImpl;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.handler.BungeeDataHandler;
import org.geysermc.floodgate.util.LanguageManager;

import java.util.UUID;

public final class BungeeListener implements Listener {
    private BungeeDataHandler dataHandler;
    @Inject private ProxyFloodgateApi api;
    @Inject private LanguageManager languageManager;
    @Inject private FloodgateLogger logger;

    @Inject
    public void init(Injector injector) {
        dataHandler = injector.getInstance(BungeeDataHandler.class);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnect(ServerConnectEvent event) {
        dataHandler.handleServerConnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(PreLoginEvent event) {
        dataHandler.handlePreLogin(event);
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
        if (player != null) {
            player.as(FloodgatePlayerImpl.class).setLogin(false);
            logger.info(languageManager.getLogString("floodgate.ingame.login_name",
                    player.getCorrectUsername(), player.getCorrectUniqueId()));
            languageManager.loadLocale(player.getLanguageCode());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginMonitor(LoginEvent event) {
        if (event.isCancelled()) {
            api.removePlayer(event.getConnection().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (api.removePlayer(player.getUniqueId()) != null) {
            api.removeEncryptedData(player.getUniqueId());
            logger.info(languageManager.getLogString(
                    "floodgate.ingame.disconnect_name", player.getName())
            );
        }
    }
}
