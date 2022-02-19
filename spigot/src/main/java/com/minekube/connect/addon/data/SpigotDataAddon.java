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
import com.minekube.connect.api.SimpleFloodgateApi;
import com.minekube.connect.api.inject.InjectorAddon;
import com.minekube.connect.api.logger.FloodgateLogger;
import com.minekube.connect.config.FloodgateConfig;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.player.FloodgateHandshakeHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public final class SpigotDataAddon implements InjectorAddon {
    @Inject private FloodgateHandshakeHandler handshakeHandler;
    @Inject private FloodgateConfig config;
    @Inject private SimpleFloodgateApi api;
    @Inject private FloodgateLogger logger;

    @Inject
    @Named("packetHandler")
    private String packetHandlerName;

    @Inject
    @Named("kickMessageAttribute")
    private AttributeKey<String> kickMessageAttribute; // TODO remove put into session context?

    @Override
    public void onInject(Channel channel, boolean toServer) {
        // At this point channel must be a local session
        LocalSession.context(channel, ctx -> {
            // we have to add the packet blocker in the data handler, otherwise ProtocolSupport breaks
            channel.pipeline().addBefore(
                    packetHandlerName, "floodgate_data_handler",
                    new SpigotDataHandler(ctx,
                            packetHandlerName,
                            handshakeHandler,
                            config,
                            kickMessageAttribute)
            );
            channel.pipeline().names().forEach(System.out::println);
//            channel.pipeline().addFirst(
//                    new ChannelOutboundHandlerAdapter() {
//                        @Override
//                        public void write(ChannelHandlerContext ctx, Object msg,
//                                          ChannelPromise promise) throws Exception {
//
//                            System.out.println("write " + msg.getClass() + " " + msg);
//                            super.write(ctx, msg, promise);
//                        }
//
//                    });
//            if (ProxyUtils.isVelocitySupport()) {
//                System.out.println("handle velocity");
//
//                channel.pipeline().addAfter("encoder", "floodgate_velocity_data",
//                        new ChannelOutboundHandlerAdapter() {
//                            @Override
//                            public void write(ChannelHandlerContext ctx, Object msg,
//                                              ChannelPromise promise) throws Exception {
//
//                                System.out.println(msg.getClass());
//                                if (ClassNames.PLUGIN_MESSAGE_OUT_PACKET.isInstance(msg)) {
//                                    System.out.println("server -> client: velocity login request");
//                                    System.out.println(
//                                            getValue(msg, ClassNames.PLUGIN_MESSAGE_OUT_ID));
//                                    System.out.println(
//                                            getValue(msg, ClassNames.PLUGIN_MESSAGE_OUT_CHANNEL));
//                                }
//
//                                System.out.println("write " + msg.getClass() + " " + msg);
//                                super.write(ctx, msg, promise);
//                            }
//
//                        });
//
//            }
        });
    }

    @Override
    public void onChannelClosed(Channel channel) {
        System.out.println("server side player channel closed");
        LocalSession.context(channel, ctx -> {
            // TODO test if we get this message
            System.out.println("and got local session context! NICE!!!");
            if (api.setPendingRemove(ctx.getPlayer())) {
                logger.translatedInfo("floodgate.ingame.disconnect_name",
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
