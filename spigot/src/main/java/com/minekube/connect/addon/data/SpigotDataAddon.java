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
import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.network.netty.LocalSession;
import io.netty.channel.Channel;

public final class SpigotDataAddon implements InjectorAddon {
    @Inject private ConnectConfig config;
    @Inject private SimpleConnectApi api;
    @Inject private ConnectLogger logger;

    @Inject
    @Named("packetHandler")
    private String packetHandlerName;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        // At this point channel must be a local session
        LocalSession.context(channel, ctx -> {
            // we have to add the packet blocker in the data handler, otherwise ProtocolSupport breaks
            channel.pipeline().addBefore(
                    packetHandlerName, "connect_data_handler",
                    new SpigotDataHandler(ctx,
                            packetHandlerName,
                            config)
            );
        });
    }

    @Override
    public void onChannelClosed(Channel channel) {
        System.out.println("server side player channel closed");
        LocalSession.context(channel, ctx -> {
            // TODO test if we get this message
            System.out.println("and got local session context! NICE!!!");
            if (api.setPendingRemove(ctx.getPlayer())) {
                logger.translatedInfo("connect.ingame.disconnect_name",
                        ctx.getPlayer().getUsername());
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
