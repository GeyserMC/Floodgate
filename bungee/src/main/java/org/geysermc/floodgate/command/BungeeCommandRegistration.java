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

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.floodgate.BungeePlugin;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class BungeeCommandRegistration implements CommandRegistration {
    private final BungeePlugin plugin;
    private final CommandUtil commandUtil;
    private final LanguageManager languageManager;

    @Override
    public void register(Command command) {
        String defaultLocale = languageManager.getDefaultLocale();

        ProxyServer.getInstance().getPluginManager().registerCommand(
                plugin, new BungeeCommandWrapper(command, commandUtil, defaultLocale)
        );
    }

    protected static class BungeeCommandWrapper extends net.md_5.bungee.api.plugin.Command {
        private final Command command;
        private final CommandUtil commandUtil;
        private final String defaultLocale;

        public BungeeCommandWrapper(Command command, CommandUtil commandUtil,
                                    String defaultLocale) {
            super(command.getName());
            this.command = command;
            this.commandUtil = commandUtil;
            this.defaultLocale = defaultLocale;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                if (command.isRequirePlayer()) {
                    commandUtil.sendMessage(
                            sender, defaultLocale,
                            CommonCommandMessage.NOT_A_PLAYER
                    );
                    return;
                }
                command.execute(sender, defaultLocale, args);
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;
            Locale locale = player.getLocale();
            String localeString = locale.getLanguage() + "_" + locale.getCountry();

            command.execute(sender, player.getUniqueId(), player.getName(), localeString, args);
        }
    }
}
