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

package org.geysermc.floodgate.command.main;

import static org.geysermc.floodgate.util.Constants.COLOR_CHAR;

import java.util.Locale;
import org.geysermc.floodgate.command.util.Permission;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.platform.command.SubCommands;
import org.geysermc.floodgate.player.UserAudience;
import org.incendo.cloud.Command;
import org.incendo.cloud.Command.Builder;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

public final class MainCommand extends SubCommands implements FloodgateCommand {
    public MainCommand() {
        defineSubCommand(FirewallCheckSubcommand.class);
        defineSubCommand(VersionSubcommand.class);
    }

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        Builder<UserAudience> builder = commandManager.commandBuilder(
                "floodgate",
                Description.of("A set of Floodgate related actions in one command"))
                .senderType(UserAudience.class)
                .permission(Permission.COMMAND_MAIN.get())
                .handler(this::execute);

        for (FloodgateSubCommand subCommand : subCommands()) {
            commandManager.command(builder
                    .literal(subCommand.name().toLowerCase(Locale.ROOT), Description.of(subCommand.description()))
                    .permission(subCommand.permission().get())
                    .handler(subCommand::execute)
            );
        }

        // also register /floodgate itself
        return builder.build();
    }

    public void execute(CommandContext<UserAudience> context) {
        StringBuilder helpMessage = new StringBuilder("Available subcommands are:\n");

        for (FloodgateSubCommand subCommand : subCommands()) {
            if (context.sender().hasPermission(subCommand.permission().get())) {
                helpMessage.append('\n').append(COLOR_CHAR).append('b')
                        .append(subCommand.name().toLowerCase(Locale.ROOT))
                        .append(COLOR_CHAR).append("f - ").append(COLOR_CHAR).append('7')
                        .append(subCommand.description());
            }
        }

        context.sender().sendMessage(helpMessage.toString());
    }
}
