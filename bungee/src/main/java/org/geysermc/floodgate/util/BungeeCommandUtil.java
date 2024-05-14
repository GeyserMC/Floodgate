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

package org.geysermc.floodgate.util;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudience.ConsoleAudience;
import org.geysermc.floodgate.player.UserAudience.PlayerAudience;

public final class BungeeCommandUtil extends CommandUtil {
    private final ProxyServer server;
    private UserAudience console;

    public BungeeCommandUtil(LanguageManager manager, ProxyServer server, FloodgateApi api) {
        super(manager, api);
        this.server = server;
    }

    @Override
    public @NonNull UserAudience getUserAudience(@NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSender)) {
            throw new IllegalArgumentException("Can only work with CommandSource!");
        }
        CommandSender source = (CommandSender) sourceObj;

        if (!(source instanceof ProxiedPlayer)) {
            if (console != null) {
                return console;
            }
            return console = new ConsoleAudience(source, this);
        }

        ProxiedPlayer player = (ProxiedPlayer) source;
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        Locale playerLocale = player.getLocale(); // Is null during the PostLoginEvent, which can cause https://github.com/GeyserMC/Floodgate/issues/510
        String locale = Utils.getLocale(playerLocale != null ? playerLocale : Locale.getDefault());

        return new PlayerAudience(uuid, username, locale, source, this, true);
    }

    @Override
    protected String getUsernameFromSource(@NonNull Object source) {
        return ((ProxiedPlayer) source).getName();
    }

    @Override
    protected UUID getUuidFromSource(@NonNull Object source) {
        return ((ProxiedPlayer) source).getUniqueId();
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return server.getPlayers();
    }

    @Override
    public Object getPlayerByUuid(@NonNull UUID uuid) {
        ProxiedPlayer player = server.getPlayer(uuid);
        return player != null ? player : uuid;
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        ProxiedPlayer player = server.getPlayer(username);
        return player != null ? player : username;
    }

    @Override
    public boolean hasPermission(Object player, String permission) {
        return ((CommandSender) player).hasPermission(permission);
    }

    @Override
    public void sendMessage(Object target, String message) {
        ((CommandSender) target).sendMessage(message);
    }

    @Override
    public void kickPlayer(Object player, String message) {
        // can also be a console
        if (player instanceof ProxiedPlayer) {
            ((ProxiedPlayer) player).disconnect(message);
        }
    }
}
