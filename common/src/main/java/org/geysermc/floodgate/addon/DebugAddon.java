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

package org.geysermc.floodgate.addon;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.geysermc.floodgate.addon.debug.ChannelInDebugHandler;
import org.geysermc.floodgate.addon.debug.ChannelOutDebugHandler;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.util.Utils;

public final class DebugAddon implements InjectorAddon {
    @Inject private FloodgateConfig config;
    @Inject private FloodgateLogger logger;

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
        channel.pipeline().addBefore(
                packetEncoder, "floodgate_debug_out",
                new ChannelOutDebugHandler(implementationName, toServer, logger)
        ).addBefore(
                packetDecoder, "floodgate_debug_in",
                new ChannelInDebugHandler(implementationName, toServer, logger)
        );
    }

    @Override
    public void onLoginDone(Channel channel) {
        onRemoveInject(channel);
    }

    @Override
    public void onRemoveInject(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        Utils.removeHandler(pipeline, "floodgate_debug_out");
        Utils.removeHandler(pipeline, "floodgate_debug_in");
    }

    @Override
    public boolean shouldInject() {
        return config.isDebug();
    }
}
