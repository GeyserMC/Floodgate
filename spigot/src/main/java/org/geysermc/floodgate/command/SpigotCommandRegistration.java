/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class SpigotCommandRegistration implements CommandRegistration {
    private final JavaPlugin plugin;
    private final CommandUtil commandUtil;
    private final LanguageManager languageManager;

    @Override
    public void register(Command command) {
        String defaultLocale = languageManager.getDefaultLocale();

        plugin.getCommand(command.getName()).setExecutor(
                new SpigotCommandWrapper(commandUtil, command, defaultLocale)
        );
    }

    @RequiredArgsConstructor
    protected static class SpigotCommandWrapper implements CommandExecutor {
        private final CommandUtil commandUtil;
        private final Command command;
        private final String defaultLocale;

        @Override
        public boolean onCommand(CommandSender source, org.bukkit.command.Command cmd,
                                 String label, String[] args) {
            if (!(source instanceof Player)) {
                if (command.isRequirePlayer()) {
                    commandUtil.sendMessage(
                            source, defaultLocale,
                            CommonCommandMessage.NOT_A_PLAYER
                    );
                    return true;
                }
                command.execute(source, defaultLocale, args);
                return true;
            }

            Player player = (Player) source;
            String locale = player.spigot().getLocale();

            command.execute(source, player.getUniqueId(), source.getName(), locale, args);
            return true;
        }
    }
}
