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

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel.Identity;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannel.Result;

@RequiredArgsConstructor
public class VelocityPluginMessageUtils extends PluginMessageUtils {
    private final PluginMessageManager pluginMessageManager;
    private ProxyServer proxy;
    private FloodgateLogger logger;

    @Inject // called because this is a listener as well
    public void init(ProxyServer proxy, FloodgateLogger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String channelId = event.getIdentifier().getId();
        PluginMessageChannel channel = pluginMessageManager.getChannel(channelId);
        if (channel == null) {
            return;
        }

        UUID targetUuid = null;
        String targetUsername = null;
        Identity targetIdentity = Identity.UNKNOWN;

        ChannelMessageSink target = event.getTarget();
        if (target instanceof Player) {
            Player player = (Player) target;
            targetUuid = player.getUniqueId();
            targetUsername = player.getUsername();
            targetIdentity = Identity.PLAYER;

        } else if (target instanceof ServerConnection) {
            targetIdentity = Identity.SERVER;
        }

        UUID sourceUuid = null;
        String sourceUsername = null;
        Identity sourceIdentity = Identity.UNKNOWN;

        ChannelMessageSource source = event.getSource();
        if (source instanceof Player) {
            Player player = (Player) source;
            sourceUuid = player.getUniqueId();
            sourceUsername = player.getUsername();
            sourceIdentity = Identity.PLAYER;

        } else if (source instanceof ServerConnection) {
            sourceIdentity = Identity.SERVER;
        }

        Result result = channel.handleProxyCall(event.getData(), targetUuid, targetUsername,
                targetIdentity, sourceUuid, sourceUsername, sourceIdentity);

        event.setResult(result.isAllowed() ? ForwardResult.forward() : ForwardResult.handled());

        if (!result.isAllowed() && result.getReason() != null) {
            logKick(source, result.getReason());
        }
    }

    private void logKick(ChannelMessageSource source, String reason) {
        logger.error(reason + " Closing connection");
        ((Player) source).disconnect(Component.text(reason));
    }

    public boolean sendMessage(
            UUID player,
            boolean toServer,
            ChannelIdentifier identifier,
            byte[] data) {

        if (toServer) {
            return proxy.getPlayer(player)
                    .flatMap(Player::getCurrentServer)
                    .map(server -> server.sendPluginMessage(identifier, data))
                    .orElse(false);
        }

        return proxy.getPlayer(player)
                .map(value -> value.sendPluginMessage(identifier, data))
                .orElse(false);
    }

    @Override
    public boolean sendMessage(UUID player, boolean toServer, String channel, byte[] data) {
        return sendMessage(player, toServer, MinecraftChannelIdentifier.from(channel), data);
    }
}
