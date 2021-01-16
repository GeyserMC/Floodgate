/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.player;

import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.CommandMessage;

public interface UserAudience extends Identified, Identity, Audience {
    @Override
    @NonNull UUID uuid();

    @NonNull String username();

    @NonNull String locale();

    @NonNull Object source();

    boolean hasPermission(@NonNull final String permission);

    @Override
    void sendMessage(final @NonNull Identity source,
                     final @NonNull Component message,
                     final @NonNull MessageType type);

    void sendMessage(CommandMessage message, Object... args);

    void disconnect(@NonNull final Component reason);

    void disconnect(CommandMessage message, Object... args);

    @Override
    default @NonNull Identity identity() {
        return this;
    }

    interface PlayerAudience extends UserAudience {
        boolean online();
    }

    interface ConsoleAudience extends UserAudience {

    }
}
