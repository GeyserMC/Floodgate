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

package org.geysermc.floodgate.util;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.util.CommandResponseCache;
import org.geysermc.floodgate.platform.command.util.CommandUtil;

@RequiredArgsConstructor
public final class BungeeCommandUtil extends CommandResponseCache<BaseComponent[]> implements CommandUtil {
    private final FloodgateLogger logger;

    @Override
    public void sendMessage(Object player, CommandMessage message, Object... args) {
        cast(player).sendMessage(getOrAddCachedMessage(message, args));
    }

    @Override
    public void kickPlayer(Object player, CommandMessage message, Object... args) {
        cast(player).disconnect(getOrAddCachedMessage(message, args));
    }

    @Override
    protected BaseComponent[] transformMessage(String message) {
        return TextComponent.fromLegacyText(message);
    }

    protected ProxiedPlayer cast(Object player) {
        try {
            return (ProxiedPlayer) player;
        } catch (ClassCastException exception) {
            logger.error("Failed to cast {} to ProxiedPlayer", player.getClass().getName());
            throw exception;
        }
    }
}
