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

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandRegistration;
import org.geysermc.floodgate.util.VelocityCommandUtil;

import java.util.UUID;

@RequiredArgsConstructor
public final class VelocityCommandRegistration implements CommandRegistration {
    private final CommandManager commandManager;
    private final VelocityCommandUtil commandUtil;

    @Override
    public void register(Command command) {
        commandManager.register(command.getName(),
                new VelocityCommandWrapper(commandUtil, command));
    }

    @RequiredArgsConstructor
    protected static final class VelocityCommandWrapper
            implements com.velocitypowered.api.command.Command {
        private final VelocityCommandUtil commandUtil;
        private final Command command;

        @Override
        public void execute(CommandSource source, @NonNull String[] args) {
            if (!(source instanceof Player)) {
                if (command.isRequirePlayer()) {
                    commandUtil.sendMessage(source, CommonCommandMessage.NOT_A_PLAYER);
                    return;
                }
                command.execute(source, args);
                return;
            }

            Player player = (Player) source;
            UUID uuid = player.getUniqueId();
            String username = player.getUsername();
            command.execute(source, uuid, username, args);
        }
    }
}
