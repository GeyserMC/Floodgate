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

import java.util.Locale;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

public abstract class FloodgateSubCommand {
    private final Class<?> parent;
    private final String name;
    private final String description;
    private final Permission permission;
    private final String[] aliases;

    protected FloodgateSubCommand(Class<?> parent, String name, String description, Permission permission, String... aliases) {
        this.parent = Objects.requireNonNull(parent);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.permission = permission;
        this.aliases = Objects.requireNonNull(aliases);
    }

    protected FloodgateSubCommand(Class<?> parent, String name, String description, String... aliases) {
        this(parent, name, description, null, aliases);
    }

    public Command.Builder<UserAudience> onBuild(Command.Builder<UserAudience> commandBuilder) {
        var builder = commandBuilder;
        if (permission != null) {
            builder =  builder.permission(permission.get());
        }
        return builder.literal(name.toLowerCase(Locale.ROOT), Description.of(description), aliases)
                .handler(this::execute);
    }

    public abstract void execute(CommandContext<UserAudience> context);

    public Class<?> parent() {
        return parent;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public @Nullable Permission permission() {
        return permission;
    }
}
