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
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudience.ConsoleAudience;
import org.geysermc.floodgate.player.UserAudience.PlayerAudience;

public final class SpigotCommandUtil extends CommandUtil {
    private final Server server;
    private final SpigotVersionSpecificMethods versionSpecificMethods;
    private UserAudience console;

    public SpigotCommandUtil(
            LanguageManager manager,
            Server server,
            FloodgateApi api,
            SpigotVersionSpecificMethods versionSpecificMethods
    ) {
        super(manager, api);
        this.server = server;
        this.versionSpecificMethods = versionSpecificMethods;
    }

    @Override
    public @NonNull UserAudience getUserAudience(final @NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSender)) {
            throw new IllegalArgumentException("Source has to be a CommandSender!");
        }
        CommandSender source = (CommandSender) sourceObj;

        if (!(source instanceof Player)) {
            if (console != null) {
                return console;
            }
            return console = new ConsoleAudience(source, this);
        }

        Player player = (Player) source;
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        String locale = versionSpecificMethods.getLocale(player);

        return new PlayerAudience(uuid, username, locale, source,this, true);
    }

    @Override
    protected String getUsernameFromSource(@NonNull Object source) {
        return ((Player) source).getName();
    }

    @Override
    protected UUID getUuidFromSource(@NonNull Object source) {
        return ((Player) source).getUniqueId();
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return server.getOnlinePlayers();
    }

    @Override
    public Object getPlayerByUuid(@NonNull UUID uuid) {
        Player player = server.getPlayer(uuid);
        return player != null ? player : uuid;
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        Player player = server.getPlayer(username);
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
        // can also be console
        if (player instanceof Player) {
            versionSpecificMethods.schedule(() -> ((Player) player).kickPlayer(message), 0);
        }
    }

    @Override
    public boolean whitelistPlayer(UUID uuid, String username) {
        return WhitelistUtils.addPlayer(uuid, username, versionSpecificMethods);
    }

    @Override
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        return WhitelistUtils.removePlayer(uuid, username, versionSpecificMethods);
    }
}
