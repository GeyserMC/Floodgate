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

package org.geysermc.floodgate.addon.addonmanager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.inject.CommonPlatformInjector;

@RequiredArgsConstructor
public final class AddonManagerHandler extends MessageToByteEncoder<ByteBuf> {
    private final CommonPlatformInjector injector;
    private final Channel channel;

    private boolean loggedIn;
    private boolean removed;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // without this check, addons will be removed twice
        if (!removed) {
            removed = true;
            injector.removeAddonsCall(channel);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        if (loggedIn) {
            out.writeBytes(msg);
            return;
        }

        int index = msg.readerIndex();
        // LoginSuccess packet = 2
        if (readVarInt(msg) == 2) {
            loggedIn = true;
            injector.loginSuccessCall(channel);
        }

        msg.readerIndex(index);
        out.writeBytes(msg);
    }

    private int readVarInt(ByteBuf buffer) {
        int out = 0;
        int count = 0;
        byte current;
        do {
            current = buffer.readByte();
            out |= (current & 0x7F) << (count++ * 7);

            if (count > 5) {
                throw new RuntimeException("VarInt is bigger then allowed");
            }
        } while ((current & 0x80) != 0);
        return out;
    }
}
