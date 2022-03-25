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

package com.minekube.connect.command.main;

import static com.minekube.connect.util.Constants.COLOR_CHAR;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.Command.Builder;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.minekube.connect.command.util.Permission;
import com.minekube.connect.platform.command.ConnectCommand;
import com.minekube.connect.player.UserAudience;
import java.util.Locale;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

public final class MainCommand implements ConnectCommand {
    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        Builder<UserAudience> builder = commandManager.commandBuilder(
                        "floodgate",
                        ArgumentDescription.of("A set of Floodgate related actions in one command"))
                .senderType(UserAudience.class)
                .permission(Permission.COMMAND_MAIN.get())
                .handler(this::execute);

        for (SubCommand subCommand : SubCommand.VALUES) {
            commandManager.command(builder
                    .literal(subCommand.name().toLowerCase(Locale.ROOT), subCommand.description)
                    .permission(subCommand.permission.get())
                    .handler(subCommand.executor::accept)
            );
        }

        // also register /floodgate itself
        return builder.build();
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        StringBuilder helpMessage = new StringBuilder("Available subcommands are:\n");

        for (SubCommand subCommand : SubCommand.VALUES) {
            if (context.getSender().hasPermission(subCommand.permission.get())) {
                helpMessage.append('\n').append(COLOR_CHAR).append('b')
                        .append(subCommand.name().toLowerCase(Locale.ROOT))
                        .append(COLOR_CHAR).append("f - ").append(COLOR_CHAR).append('7')
                        .append(subCommand.description);
            }
        }

        context.getSender().sendMessage(helpMessage.toString());
    }

    @RequiredArgsConstructor
    enum SubCommand {
        FIREWALL("Check if your outgoing firewall allows Floodgate to work properly",
                Permission.COMMAND_MAIN_FIREWALL, FirewallCheckSubcommand::executeFirewall);

        static final SubCommand[] VALUES = values();

        final String description;
        final Permission permission;
        final Consumer<CommandContext<UserAudience>> executor;
    }
}
