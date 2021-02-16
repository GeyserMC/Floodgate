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

package org.geysermc.floodgate.util;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;

@RequiredArgsConstructor
public class BungeeUserAudience implements UserAudience, ForwardingAudience.Single {
    private final UUID uuid;
    private final String locale;
    private final CommandSender source;
    private final CommandUtil commandUtil;

    @Override
    public @NonNull UUID uuid() {
        return uuid;
    }

    @Override
    public @NonNull String username() {
        return source.getName();
    }

    @Override
    public @NonNull String locale() {
        return locale;
    }

    @Override
    public @NonNull CommandSender source() {
        return source;
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(@NonNull Identity source, @NonNull Component message,
                            @NonNull MessageType type) {
        this.source.sendMessage(GsonComponentSerializer.gson().serialize(message));
    }

    @Override
    public void sendMessage(CommandMessage message, Object... args) {
        commandUtil.sendMessage(source(), locale(), message, args);
    }

    @Override
    public void disconnect(@NonNull Component reason) {
        if (source instanceof ProxiedPlayer) {
            ((ProxiedPlayer) source).disconnect(GsonComponentSerializer.gson().serialize(reason));
        }
    }

    @Override
    public void disconnect(CommandMessage message, Object... args) {
        commandUtil.kickPlayer(source(), locale(), message, args);
    }

    @Override
    public @NonNull Audience audience() {
        return this;
    }

    public static final class BungeeConsoleAudience extends BungeeUserAudience
            implements ConsoleAudience {

        public BungeeConsoleAudience(CommandSender source, CommandUtil commandUtil) {
            super(new UUID(0, 0), "en_us", source, commandUtil);
        }

        @Override
        public void sendMessage(
                @NonNull Identity source,
                @NonNull Component message,
                @NonNull MessageType type) {
            source().sendMessage(BungeeComponentSerializer.get().serialize(message));
        }
    }

    public static final class BungeePlayerAudience extends BungeeUserAudience
            implements PlayerAudience {

        private final String username;
        private final boolean online;

        public BungeePlayerAudience(
                UUID uuid,
                String username,
                String locale,
                CommandSender source,
                boolean online,
                CommandUtil commandUtil) {
            super(uuid, locale, source, commandUtil);
            this.username = username;
            this.online = online;
        }

        public BungeePlayerAudience(
                UUID uuid,
                String locale,
                CommandSender source,
                boolean online,
                CommandUtil commandUtil) {
            this(uuid, source.getName(), locale, source, online, commandUtil);
        }

        @Override
        public @NonNull String username() {
            return username;
        }

        @Override
        public boolean online() {
            return online;
        }
    }
}
