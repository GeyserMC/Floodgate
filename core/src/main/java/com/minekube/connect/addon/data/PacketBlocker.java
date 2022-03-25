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

import com.google.common.collect.Queues;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Queue;

/**
 * PacketBlocker is used to temporarily prevent packets from being decoded. A similar system is used
 * to prevent packets from being handled while Connect is processing the login. The old system
 * blocked the thread which was processing the Connect login, but that doesn't only block the
 * packets for that specific user, it's shared between multiple users causing them to lag or
 * sometimes timeout.
 * <br>
 * The reason why we prevent packets from being handled is because keeping the packet order is
 * important. That is also the reason why we prevent packets from being decoded during that time.
 * The 'login start' packet for example can only be decoded after the handshake packet has been
 * handled, because the server can only successfully decode the packet after the handshake packet
 * caused the server to switch to the login state.
 */
public class PacketBlocker extends ChannelInboundHandlerAdapter {
    private final Queue<Object> packetQueue = Queues.newConcurrentLinkedQueue();
    private volatile boolean blockPackets;

    private ChannelHandlerContext ctx;

    public void enable() {
        blockPackets = true;
    }

    public void disable() {
        blockPackets = false;

        Object packet;
        while ((packet = packetQueue.poll()) != null) {
            ctx.fireChannelRead(packet);
        }
        ctx.pipeline().remove(this);
    }

    public boolean enabled() {
        return blockPackets;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (blockPackets || !packetQueue.isEmpty()) {
            packetQueue.add(msg);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
}
