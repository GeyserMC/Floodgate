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

package org.geysermc.floodgate.listener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.handler.BungeeDataHandler;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.pluginmessage.BungeePluginMessageHandler;
import org.geysermc.floodgate.skin.SkinHandler;
import org.geysermc.floodgate.util.BungeeCommandUtil;
import org.geysermc.floodgate.util.LanguageManager;

public final class BungeeListener implements Listener {
    private BungeeDataHandler dataHandler;
    @Inject private ProxyFloodgateApi api;
    @Inject private LanguageManager languageManager;
    @Inject private FloodgateLogger logger;

    @Inject private ProxyFloodgateConfig config;
    @Inject private BungeePluginMessageHandler pluginMessageHandler;
    @Inject private SkinHandler skinHandler;

    @Inject
    public void init(Injector injector) {
        dataHandler = injector.getInstance(BungeeDataHandler.class);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnect(ServerConnectEvent event) {
        dataHandler.handleServerConnect(event.getPlayer());
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        FloodgatePlayer player = api.getPlayer(event.getPlayer().getUniqueId());
        if (player == null) {
            return;
        }

        // only ask for skin upload if it hasn't been uploaded already
        if (player.hasProperty(PropertyKey.SKIN_UPLOADED)) {
            return;
        }

        // send skin request to server if data forwarding allows that
        if (config.isSendFloodgateData()) {
            pluginMessageHandler.sendSkinRequest(event.getServer(), player.getRawSkin());
        } else {
            skinHandler.handleSkinUploadFor(player, null);
        }
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
            logger.translatedInfo(
                    "floodgate.ingame.login_name",
                    player.getCorrectUsername(), uniqueId
            );
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

        BungeeCommandUtil.AUDIENCE_CACHE.remove(player.getUniqueId()); //todo

        if (api.removePlayer(player.getUniqueId()) != null) {
            api.removeEncryptedData(player.getUniqueId());
            logger.translatedInfo("floodgate.ingame.disconnect_name", player.getName());
        }
    }
}
