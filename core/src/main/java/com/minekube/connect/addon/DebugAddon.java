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

package com.minekube.connect.addon;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.minekube.connect.addon.debug.ChannelInDebugHandler;
import com.minekube.connect.addon.debug.ChannelOutDebugHandler;
import com.minekube.connect.addon.debug.StateChangeDetector;
import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConnectConfig;
import com.minekube.connect.util.Utils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public final class DebugAddon implements InjectorAddon {
    @Inject private ConnectConfig config;
    @Inject private ConnectLogger logger;

    @Inject
    @Named("implementationName")
    private String implementationName;

    @Inject
    @Named("packetEncoder")
    private String packetEncoder;

    @Inject
    @Named("packetDecoder")
    private String packetDecoder;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        logger.info("Successfully called onInject. To server? " + toServer);

        StateChangeDetector changeDetector = new StateChangeDetector(
                channel, packetEncoder, packetDecoder, logger
        );

        channel.pipeline().addBefore(
                packetEncoder, "connect_debug_out",
                new ChannelOutDebugHandler(implementationName, toServer, changeDetector, logger)
        ).addBefore(
                packetDecoder, "connect_debug_in",
                new ChannelInDebugHandler(implementationName, toServer, changeDetector, logger)
        );
    }

    @Override
    public void onRemoveInject(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        Utils.removeHandler(pipeline, "connect_debug_out");
        Utils.removeHandler(pipeline, "connect_debug_in");
    }

    @Override
    public boolean shouldInject() {
        return config.isDebug();
    }
}
