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
import com.minekube.connect.addon.packethandler.ChannelInPacketHandler;
import com.minekube.connect.addon.packethandler.ChannelOutPacketHandler;
import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.packet.PacketHandlersImpl;
import com.minekube.connect.util.Utils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public class PacketHandlerAddon implements InjectorAddon {
    @Inject private PacketHandlersImpl packetHandlers;

    @Inject
    @Named("packetEncoder")
    private String packetEncoder;

    @Inject
    @Named("packetDecoder")
    private String packetDecoder;

    @Override
    public void onInject(Channel channel, boolean toServer) {
        channel.pipeline().addAfter(
                packetEncoder, "connect_phaddon_out",
                new ChannelOutPacketHandler(packetHandlers, toServer)
        ).addAfter(
                packetDecoder, "connect_phaddon_in",
                new ChannelInPacketHandler(packetHandlers, toServer)
        );
    }

    @Override
    public void onRemoveInject(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        Utils.removeHandler(pipeline, "connect_phaddon_out");
        Utils.removeHandler(pipeline, "connect_phaddon_in");
    }

    @Override
    public boolean shouldInject() {
        // this means that there has to be at least one PacketHandler registered to inject
        return packetHandlers.hasHandlers();
    }
}
