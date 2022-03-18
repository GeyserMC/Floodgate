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

package org.geysermc.floodgate.player;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.command.TranslatableMessage;

@Getter @Accessors(fluent = true)
public class UserAudience {
    private final @NonNull UUID uuid;
    private final @NonNull String username;
    private final @NonNull String locale;
    private final @NonNull Object source;
    private final @NonNull CommandUtil commandUtil;

    public UserAudience(
            @NonNull UUID uuid,
            @NonNull String username,
            @NonNull String locale,
            @NonNull Object source,
            @NonNull CommandUtil commandUtil) {
        this.uuid = Objects.requireNonNull(uuid);
        this.username = username;
        this.locale = Objects.requireNonNull(locale);
        this.source = Objects.requireNonNull(source);
        this.commandUtil = Objects.requireNonNull(commandUtil);
    }

    public boolean hasPermission(@NonNull String permission) {
        return commandUtil.hasPermission(source(), permission);
    }

    public void sendMessage(String message) {
        commandUtil.sendMessage(source(), message);
    }

    public void sendMessage(TranslatableMessage message, Object... args) {
        sendMessage(translateMessage(message, args));
    }

    public void disconnect(@NonNull String reason) {
        commandUtil.kickPlayer(source(), reason);
    }

    public void disconnect(TranslatableMessage message, Object... args) {
        disconnect(translateMessage(message, args));
    }

    public String translateMessage(TranslatableMessage message, Object... args) {
        return commandUtil.translateMessage(locale(), message, args);
    }

    @Getter @Accessors(fluent = true)
    public static class PlayerAudience extends UserAudience {
        private final boolean online;

        public PlayerAudience(
                @NonNull UUID uuid,
                @NonNull String username,
                @NonNull String locale,
                @NonNull Object source,
                @NonNull CommandUtil commandUtil,
                boolean online) {
            super(uuid, username, locale, source, commandUtil);

            this.online = online;
        }
    }

    @Getter @Accessors(fluent = true)
    public static class ConsoleAudience extends UserAudience {
        public ConsoleAudience(@NonNull Object source, @NonNull CommandUtil commandUtil) {
            super(new UUID(0, 0), "CONSOLE", "en_us", source, commandUtil);
        }
    }
}
