/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.platform.command;

import static org.geysermc.floodgate.core.util.Constants.COLOR_CHAR;
import static org.incendo.cloud.description.Description.description;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.util.Locale;
import java.util.Set;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public abstract class SubCommands implements FloodgateCommand {
    private final String name;
    private final String description;
    private final Permission permission;

    @Inject Set<FloodgateSubCommand> subCommands;

    protected SubCommands(String name, String description, Permission permission) {
        this.name = name;
        this.description = description;
        this.permission = permission;
    }

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        var builder = commandManager
                .commandBuilder(name, description(description))
                .senderType(UserAudience.class)
                .permission(permission.get())
                .handler(this::execute);

        for (FloodgateSubCommand command : subCommands) {
            commandManager.command(command.onBuild(builder));
        }

        // also register /floodgate itself
        return builder.build();
    }

    public void execute(CommandContext<UserAudience> context) {
        StringBuilder helpMessage = new StringBuilder("Available subcommands are:\n");

        for (FloodgateSubCommand subCommand : subCommands) {
            var permission = subCommand.permission();
            if (permission == null || context.sender().hasPermission(permission.get())) {
                helpMessage.append('\n').append(COLOR_CHAR).append('b')
                        .append(subCommand.name().toLowerCase(Locale.ROOT))
                        .append(COLOR_CHAR).append("f - ").append(COLOR_CHAR).append('7')
                        .append(subCommand.description());
            }
        }

        context.sender().sendMessage(helpMessage.toString());
    }

    @PostConstruct
    public void setup() {
        subCommands.removeIf(subCommand -> !subCommand.parent().isAssignableFrom(this.getClass()));
    }
}
