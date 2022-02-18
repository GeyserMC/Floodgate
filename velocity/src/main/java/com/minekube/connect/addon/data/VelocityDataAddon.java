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

package com.minekube.connect.addon.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.minekube.connect.api.ProxyFloodgateApi;
import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.config.ProxyFloodgateConfig;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.network.netty.LocalSession.Context;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.Channel;

public final class VelocityDataAddon implements InjectorAddon {
    @Inject private ProxyFloodgateConfig config;
    @Inject private ProxyFloodgateApi api;
    @Inject private ProxyServer proxy;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    private String packetHandler;

    @Inject
    @Named("packetDecoder")
    private String packetDecoder;

    @Inject
    @Named("packetEncoder")
    private String packetEncoder;

    @Override
    public void onInject(Channel channel, boolean toServer) {
//        if (toServer) {
//            if (config.isSendFloodgateData()) {
//                channel.pipeline().addAfter(
//                        packetEncoder, "floodgate_data_handler",
//                        new VelocityServerDataHandler(api, proxy)
//                );
//            }
//            return;
//        }

//        PacketBlocker blocker = new PacketBlocker();
//        channel.pipeline().addBefore(packetDecoder, "floodgate_packet_blocker", blocker);

        // The handler is already added so we should add our handler before it
//        channel.pipeline().addBefore(
//                packetHandler, "floodgate_data_handler",
//                new VelocityProxyDataHandler(config, handshakeHandler, blocker,
//                        kickMessageAttribute, logger)
//        );
    }

    @Override
    public void onChannelClosed(Channel channel) {
        LocalSession.context(channel).map(Context::getPlayer).ifPresent(player -> {
            if (api.setPendingRemove(player)) {
                logger.translatedInfo("floodgate.ingame.disconnect_name", player.getUsername());
            }
        });
    }

    @Override
    public void onRemoveInject(Channel channel) {
    }

    @Override
    public boolean shouldInject() {
        return true;
    }
}
