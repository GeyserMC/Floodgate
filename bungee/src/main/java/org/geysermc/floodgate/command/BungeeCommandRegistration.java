/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.command;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.floodgate.BungeePlugin;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandRegistration;

import java.util.UUID;

@RequiredArgsConstructor
public final class BungeeCommandRegistration implements CommandRegistration {
    private final BungeePlugin plugin;

    @Override
    public void register(Command command) {
        ProxyServer.getInstance().getPluginManager().registerCommand(
                plugin, new BungeeCommandWrapper(command)
        );
    }

    protected static class BungeeCommandWrapper extends net.md_5.bungee.api.plugin.Command {
        private final Command command;

        public BungeeCommandWrapper(Command command) {
            super(command.getName());
            this.command = command;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                if (command.isRequirePlayer()) {
                    //todo let it use the response cache
                    sender.sendMessage(CommonCommandMessage.NOT_A_PLAYER.getMessage());
                    return;
                }
                command.execute(sender, args);
                return;
            }

            UUID uuid = ((ProxiedPlayer) sender).getUniqueId();
            String username = sender.getName();
            command.execute(sender, uuid, username, args);
        }
    }
}
