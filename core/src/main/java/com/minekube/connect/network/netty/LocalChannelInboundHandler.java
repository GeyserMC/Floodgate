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

package com.minekube.connect.network.netty;

import com.google.rpc.Code;
import com.google.rpc.Status;
import com.minekube.connect.api.SimpleConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.network.netty.LocalSession.Context;
import com.minekube.connect.tunnel.TunnelConn;
import com.minekube.connect.tunnel.Tunneler;
import io.grpc.protobuf.StatusProto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class LocalChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Context context;
    private final ConnectLogger logger;
    private final Tunneler tunneler;
    private final SimpleConnectApi api;

    private TunnelConn tunnelConn;

    public static void onChannelClosed(Context context,
                                       SimpleConnectApi api,
                                       ConnectLogger logger
    ) {
        // Surround with try-catch to prevent exceptions when server shutdown and classes are unloaded before
        // we could use them.
        try {
            TunnelConn tunnelConn = context.getTunnelConn().getAndSet(null);
            if (tunnelConn != null) {
                tunnelConn.close();
            }

            if (api.setPendingRemove(context.getPlayer())) {
                if (!context.getPlayer().getUsername().isEmpty()) { // might be just a ping request
                    logger.translatedInfo("connect.ingame.disconnect_name",
                            context.getPlayer().getUsername());
                }
            }

            Status reason = Status.newBuilder()
                    .setCode(Code.UNKNOWN_VALUE)
                    .setMessage("local connection closed")
                    .build();

            rejectProposal(context, reason);
        } catch (Throwable ignored) {
        }
    }

    static void rejectProposal(Context context, Status reason) {
        // Reject session proposal if the tunnel was never opened.
        // Helps to prevent confusion by the watch & tunnel services
        // since a session proposal is automatically accepted a when
        // we opened the tunnel, so we should not send a reject after it.
        TunnelConn tunnelConn = context.getTunnelConn().get();
        if (tunnelConn == null || !tunnelConn.opened()) {
            context.getSessionProposal().reject(reason);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Reject session proposal in case we are still able to and connection was stopped very early.
        rejectProposal(context, StatusProto.fromThrowable(cause));
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        // Start tunnel from downstream server -> upstream TunnelService
        tunnelConn = tunneler.tunnel(
                context.getSessionProposal().getSession().getTunnelServiceAddr(),
                context.getSessionProposal().getSession().getId(),
                new TunnelHandler(logger, ctx.channel())
        );
        context.tunnelConn.set(tunnelConn);
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        // Get underlying byte array from buf without copy
        byte[] data = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        // downstream server -> local session server -> TunnelService
        tunnelConn.write(data);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        onChannelClosed(context, api, logger);
        super.channelInactive(ctx);
    }
}
