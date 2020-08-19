/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.util;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.util.CommandResponseCache;
import org.geysermc.floodgate.platform.command.util.CommandUtil;

@RequiredArgsConstructor
public final class VelocityCommandUtil extends CommandResponseCache<TextComponent> implements CommandUtil {
    private final FloodgateLogger logger;
    private final LanguageManager manager;

    @Override
    public void sendMessage(Object player, CommandMessage message, Object... args) {
        Player velocityPlayer = cast(player);
        FloodgatePlayer floodgatePlayer =
                FloodgateApi.getInstance().getPlayer(velocityPlayer.getUniqueId());
        if (floodgatePlayer != null) {
            velocityPlayer.sendMessage(
                    transformMessage(manager.getPlayerLocaleString(message.getMessage(),
                    floodgatePlayer.getLanguageCode(), args)));
        } else {
            velocityPlayer.sendMessage(
                    transformMessage(manager.getLocaleStringLog(message.getMessage(), args)));
        }
    }

    @Override
    public void kickPlayer(Object player, CommandMessage message, Object... args) {
        cast(player).disconnect(getOrAddCachedMessage(message, args));
    }

    @Override
    protected TextComponent transformMessage(String message) {
        return LegacyComponentSerializer.legacy().deserialize(message);
    }

    protected Player cast(Object instance) {
        try {
            return (Player) instance;
        } catch (ClassCastException exception) {
            logger.error("Failed to cast {} to Player", instance.getClass().getName());
            throw exception;
        }
    }
}
