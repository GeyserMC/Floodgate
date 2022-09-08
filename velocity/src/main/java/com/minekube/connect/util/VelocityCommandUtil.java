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

package com.minekube.connect.util;

import com.google.inject.Inject;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.platform.command.CommandUtil;
import com.minekube.connect.player.UserAudience;
import com.minekube.connect.player.UserAudience.ConsoleAudience;
import com.minekube.connect.player.UserAudience.PlayerAudience;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class VelocityCommandUtil extends CommandUtil {
    private static UserAudience console;

    @Inject private ProxyServer server;

    @Inject
    public VelocityCommandUtil(LanguageManager manager, ConnectApi api) {
        super(manager, api);
    }

    @Override
    public @NonNull UserAudience getUserAudience(@NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSource)) {
            throw new IllegalArgumentException("Can only work with CommandSource!");
        }
        CommandSource source = (CommandSource) sourceObj;

        if (!(source instanceof Player)) {
            if (console != null) {
                return console;
            }
            return console = new ConsoleAudience(source, this);
        }

        Player player = (Player) source;
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();
        String locale = Utils.getLocale(player.getPlayerSettings().getLocale());

        return new PlayerAudience(uuid, username, locale, source, this, true);
    }

    @Override
    protected String getUsernameFromSource(@NonNull Object source) {
        return ((Player) source).getUsername();
    }

    @Override
    protected UUID getUuidFromSource(@NonNull Object source) {
        return ((Player) source).getUniqueId();
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return server.getAllPlayers();
    }

    @Override
    public Object getPlayerByUuid(@NonNull UUID uuid) {
        Optional<Player> player = server.getPlayer(uuid);
        return player.isPresent() ? player.get() : uuid;
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        Optional<Player> player = server.getPlayer(username);
        return player.isPresent() ? player.get() : username;
    }

    @Override
    public boolean hasPermission(Object player, String permission) {
        return ((CommandSource) player).hasPermission(permission);
    }

    @Override
    public void sendMessage(Object target, String message) {
        ((CommandSource) target).sendMessage(Component.text(message));
    }

    @Override
    public void kickPlayer(Object player, String message) {
        if (player instanceof Player) {
            ((Player) player).disconnect(Component.text(message));
        }
    }
}
