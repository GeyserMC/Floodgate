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

package org.geysermc.floodgate.velocity.addon.data;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.core.addon.data.PacketBlocker;
import org.geysermc.floodgate.core.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.logger.FloodgateLogger;

@Singleton
public final class VelocityDataAddon implements InjectorAddon {
    @Inject
    DataSeeker dataSeeker;

    @Inject
    FloodgateDataHandler handshakeHandler;

    @Inject
    ProxyFloodgateConfig config;

    @Inject
    FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    String packetHandler;

    @Inject
    @Named("packetDecoder")
    String packetDecoder;

    @Inject
    @Named("packetEncoder")
    String packetEncoder;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Inject
    @Named("kickMessageAttribute")
    AttributeKey<String> kickMessageAttribute;

    @Inject
    VelocityServerDataHandler serverDataHandler;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        if (toServer) {
            if (config.sendFloodgateData()) {
                channel.pipeline().addAfter(packetEncoder, "floodgate_data_handler", serverDataHandler);
            }
            return;
        }

        PacketBlocker blocker = new PacketBlocker();
        channel.pipeline().addBefore(packetDecoder, "floodgate_packet_blocker", blocker);

        var dataHandler = new VelocityProxyDataHandler(
                dataSeeker, handshakeHandler, config, blocker, connectionAttribute, kickMessageAttribute, logger);

        // The handler is already added so we should add our handler before it
        channel.pipeline().addBefore(packetHandler, "floodgate_data_handler", dataHandler);
    }

    @Override
    public void onRemoveInject(Channel channel) {}

    @Override
    public boolean shouldInject() {
        return true;
    }
}
