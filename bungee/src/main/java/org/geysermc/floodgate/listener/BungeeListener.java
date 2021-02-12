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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
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
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.player.FloodgatePlayerImpl;
import org.geysermc.floodgate.util.BungeeCommandUtil;
import org.geysermc.floodgate.util.LanguageManager;
import org.geysermc.floodgate.util.ReflectionUtils;

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

    @Inject private ProxyFloodgateApi api;
    @Inject private LanguageManager languageManager;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("playerAttribute")
    private AttributeKey<FloodgatePlayer> playerAttribute;

    @Inject
    @Named("kickMessageAttribute")
    private AttributeKey<String> kickMessageAttribute;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(PreLoginEvent event) {
        // well, no reason to check if the player will be kicked anyway
        if (event.isCancelled()) {
            return;
        }

        PendingConnection connection = event.getConnection();

        ChannelWrapper wrapper = ReflectionUtils.getCastedValue(connection, CHANNEL_WRAPPER);
        Channel channel = wrapper.getHandle();

        // check if the player has to be kicked
        String kickReason = channel.attr(kickMessageAttribute).get();
        if (kickReason != null) {
            event.setCancelled(true);
            event.setCancelReason(kickReason);
            return;
        }

        FloodgatePlayer player = channel.attr(playerAttribute).get();
        if (player != null) {
            connection.setOnlineMode(false);
            connection.setUniqueId(player.getCorrectUniqueId());
            ReflectionUtils.setValue(connection, PLAYER_NAME, player.getCorrectUsername());
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
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        BungeeCommandUtil.AUDIENCE_CACHE.remove(event.getPlayer().getUniqueId()); //todo
    }
}
