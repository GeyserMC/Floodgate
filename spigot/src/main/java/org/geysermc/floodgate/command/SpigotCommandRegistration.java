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

import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

public final class SpigotCommandRegistration implements CommandRegistration {
    private final SpigotVersionSpecificMethods versionSpecificMethods;
    private final JavaPlugin plugin;
    private final CommandUtil commandUtil;
    private final CommandMap commandMap;

    public SpigotCommandRegistration(SpigotVersionSpecificMethods versionSpecificMethods,
                                     JavaPlugin plugin, CommandUtil commandUtil) {
        this.versionSpecificMethods = versionSpecificMethods;
        this.plugin = plugin;
        this.commandUtil = commandUtil;
        this.commandMap = ReflectionUtils.getCastedValue(Bukkit.getPluginManager(), "commandMap");
    }

    @Override
    public void register(Command command) {
        SpigotCommand spigotCommand =
                new SpigotCommand(versionSpecificMethods, plugin, commandUtil, command);
        commandMap.register("floodgate", spigotCommand);
    }

    protected static class SpigotCommand extends org.bukkit.command.Command {
        private final SpigotVersionSpecificMethods versionSpecificMethods;
        private final JavaPlugin plugin;
        private final CommandUtil commandUtil;
        private final Command command;

        protected SpigotCommand(SpigotVersionSpecificMethods versionSpecificMethods,
                                JavaPlugin plugin, CommandUtil commandUtil, Command command) {
            super(command.getName(), command.getDescription(), "", Collections.emptyList());
            this.versionSpecificMethods = versionSpecificMethods;
            this.plugin = plugin;
            this.commandUtil = commandUtil;
            this.command = command;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!plugin.isEnabled()) {
                throw new CommandException("Cannot execute command '" + commandLabel +
                        "' while the plugin is disabled");
            }
            if (!sender.hasPermission(command.getPermission())) {
                commandUtil.sendMessage(sender, null, CommonCommandMessage.NO_PERMISSION);
                return true;
            }
            executeCommand(sender, args);
            return true;
        }

        public void executeCommand(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                if (command.isRequirePlayer()) {
                    commandUtil.sendMessage(sender, null, CommonCommandMessage.NOT_A_PLAYER);
                    return;
                }
                command.execute(sender, null, args);
                return;
            }

            Player player = (Player) sender;
            String locale = versionSpecificMethods.getLocale(player);

            command.execute(sender, player.getUniqueId(), sender.getName(), locale, args);
        }
    }
}
