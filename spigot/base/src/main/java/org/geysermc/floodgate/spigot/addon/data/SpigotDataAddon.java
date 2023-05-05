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

package org.geysermc.floodgate.spigot.addon.data;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.api.SimpleFloodgateApi;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.player.FloodgateHandshakeHandler;

@Singleton
public final class SpigotDataAddon implements InjectorAddon {
    @Inject FloodgateHandshakeHandler handshakeHandler;
    @Inject FloodgateConfig config;
    @Inject SimpleFloodgateApi api;
    @Inject FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    String packetHandlerName;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<String> kickMessageAttribute;

    @Inject
    @Named("playerAttribute")
    AttributeKey<Connection> playerAttribute;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        // we have to add the packet blocker in the data handler, otherwise ProtocolSupport breaks
        channel.pipeline().addBefore(
                packetHandlerName, "floodgate_data_handler",
                new SpigotDataHandler(handshakeHandler, config, kickMessageAttribute)
        );
    }

    @Override
    public void onChannelClosed(Channel channel) {
        Connection player = channel.attr(playerAttribute).get();
        if (player != null && api.setPendingRemove(player)) {
            logger.translatedInfo("floodgate.ingame.disconnect_name", player.javaUsername());
        }
    }

    @Override
    public void onRemoveInject(Channel channel) {
    }

    @Override
    public boolean shouldInject() {
        return true;
    }
}
