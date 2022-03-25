/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package com.minekube.connect.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.minekube.connect.api.ProxyConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.player.ConnectPlayer;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.util.LanguageManager;
import com.minekube.connect.util.ReflectionUtils;
import io.netty.channel.Channel;
import java.lang.reflect.Field;
import java.util.UUID;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.netty.ChannelWrapper;

@SuppressWarnings("ConstantConditions")
public final class BungeeListener implements Listener {
    private static final Field CHANNEL_WRAPPER;
    private static final Field PLAYER_NAME;

    static {
        CHANNEL_WRAPPER =
                ReflectionUtils.getFieldOfType(InitialHandler.class, ChannelWrapper.class);
        checkNotNull(CHANNEL_WRAPPER, "ChannelWrapper field cannot be null");

        PLAYER_NAME = ReflectionUtils.getField(InitialHandler.class, "name");
        checkNotNull(PLAYER_NAME, "Initial name field cannot be null");
    }

    @Inject private ProxyConnectApi api;
    @Inject private LanguageManager languageManager;
    @Inject private ConnectLogger logger;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(PreLoginEvent event) {
        // well, no reason to check if the player will be kicked anyway
        if (event.isCancelled()) {
            return;
        }

        PendingConnection connection = event.getConnection();

        ChannelWrapper wrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);
        Channel channel = wrapper.getHandle();

        LocalSession.context(channel, ctx -> {
            connection.setOnlineMode(false);
            connection.setUniqueId(ctx.getPlayer().getUniqueId());
            ReflectionUtils.setValue(connection, PLAYER_NAME, ctx.getPlayer().getUsername());
            // TODO robin: what about profile properties? (but why is skin already showing)
        });
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        // if there was another player with the same uuid / name online,
        // he has been disconnected by now
        UUID uniqueId = event.getConnection().getUniqueId();
        ConnectPlayer player = api.getPlayer(uniqueId);
        if (player != null) {
            //todo we should probably move this log message earlier in the process, so that we know
            // that Floodgate has done its job
            logger.translatedInfo(
                    "connect.ingame.login_name",
                    player.getUsername(), uniqueId
            );
            languageManager.loadLocale(player.getLanguageTag());
        }
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onPostLogin(PostLoginEvent event) {
    // To fix the February 2 2022 Mojang authentication changes
//        ConnectPlayer player = api.getPlayer(event.getPlayer().getUniqueId());
//        if (player != null) {
//            skinApplier.applySkin(player, new SkinData("", ""));
//        }
//    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        api.playerRemoved(event.getPlayer().getUniqueId());
    }
}
