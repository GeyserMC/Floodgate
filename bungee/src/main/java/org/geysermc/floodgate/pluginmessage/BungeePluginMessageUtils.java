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

package org.geysermc.floodgate.pluginmessage;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel.Identity;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel.Result;

@RequiredArgsConstructor
public final class BungeePluginMessageUtils extends PluginMessageUtils implements Listener {
    private final PluginMessageManager pluginMessageManager;
    private final FloodgateLogger logger;

    @EventHandler(priority = EventPriority.LOW)
    public void onPluginMessage(PluginMessageEvent event) {
        PluginMessageChannel channel = pluginMessageManager.getChannel(event.getTag());
        if (channel == null) {
            return;
        }

        UUID targetUuid = null;
        String targetUsername = null;
        Identity targetIdentity = Identity.UNKNOWN;

        Connection target = event.getReceiver();
        if (target instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) target;
            targetUuid = player.getUniqueId();
            targetUsername = player.getName();
            targetIdentity = Identity.PLAYER;

        } else if (target instanceof ServerConnection) {
            targetIdentity = Identity.SERVER;
        }

        UUID sourceUuid = null;
        String sourceUsername = null;
        Identity sourceIdentity = Identity.UNKNOWN;

        Connection source = event.getSender();
        if (source instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) source;
            sourceUuid = player.getUniqueId();
            sourceUsername = player.getName();
            sourceIdentity = Identity.PLAYER;

        } else if (source instanceof ServerConnection) {
            sourceIdentity = Identity.SERVER;
        }

        Result result = channel.handleProxyCall(event.getData(), targetUuid, targetUsername,
                targetIdentity, sourceUuid, sourceUsername, sourceIdentity);

        event.setCancelled(result.isAllowed());

        if (!result.isAllowed() && result.getReason() != null) {
            logKick(source, result.getReason());
        }
    }

    private void logKick(Connection source, String reason) {
        logger.error(reason + " Closing connection");
        source.disconnect(new TextComponent(reason));
    }
}
