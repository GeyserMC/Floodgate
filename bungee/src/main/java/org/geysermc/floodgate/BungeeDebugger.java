package org.geysermc.floodgate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Varint21LengthFieldPrepender;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class BungeeDebugger {
    public BungeeDebugger() {
        init();
    }

    private void init() {
        Class<?> pipelineUtils = ReflectionUtil.getPrefixedClass("netty.PipelineUtils");
        Field framePrepender = ReflectionUtil.getField(pipelineUtils, "framePrepender");
        ReflectionUtil.setFinalValue(null, framePrepender, new CustomVarint21LengthFieldPrepender());
    }

    @ChannelHandler.Sharable
    private static class CustomVarint21LengthFieldPrepender extends Varint21LengthFieldPrepender {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            // we're getting called before the encoder and decoder are added,
            // so we have to wait with a nice while loop :D
            ctx.executor().execute(() -> {
                System.out.println("Channel: " + ctx.channel().isActive() + " " + ctx.channel().isOpen() + " " + ctx.channel().isRegistered());
                while (ctx.channel().isOpen()) {
                    System.out.println("Trying to find decoder for " + getHostString(ctx, true) + " " + getParentName(ctx, true));
                    if (ctx.channel().pipeline().get(MinecraftEncoder.class) != null) {
                        System.out.println("Found decoder for " + getHostString(ctx, true));
                        ctx.channel().pipeline().addLast(
                                "floodgate-debug-init",
                                new BungeeChannelInitializer(
                                        ctx.channel().parent() instanceof ServerSocketChannel
                                )
                        );
                        break;
                    }
                }
            });
        }
    }

    public static String getHostString(ChannelHandlerContext ctx, boolean alwaysString) {
        SocketAddress address = ctx.channel().remoteAddress();
        return address != null ? ((InetSocketAddress) address).getHostString() : (alwaysString ? "null" : null);
    }

    public static String getParentName(ChannelHandlerContext ctx, boolean alwaysString) {
        Channel parent = ctx.channel().parent();
        return parent != null ? parent.getClass().getSimpleName() : (alwaysString ? "null" : null);
    }

    public static int executorsCount(ChannelHandlerContext ctx) {
        return ((MultithreadEventLoopGroup) ctx.channel().eventLoop().parent()).executorCount();
    }

    @RequiredArgsConstructor
    private static class BungeeChannelInitializer extends ChannelInitializer<Channel> {
        private final boolean player;

        @Override
        protected void initChannel(Channel channel) throws Exception {
            System.out.println("Init " + (player ? "Bungee" : "Server"));
            // can't add our debugger when the inbound-boss is missing
            if (channel.pipeline().get("packet-encoder") == null) return;
            if (channel.pipeline().get("packet-decoder") == null) return;
            channel.pipeline()
                    .addBefore("packet-decoder", "floodgate-debug-in", new BungeeInPacketHandler(player))
                    .addBefore("packet-encoder", "floodgate-debug-out", new BungeeOutPacketHandler(player));
        }
    }

    @RequiredArgsConstructor
    private static class BungeeInPacketHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final boolean player;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            int index = msg.readerIndex();
            System.out.println((player ? "Player -> Bungee" : "Server -> Bungee") + ":\n" + ByteBufUtil.prettyHexDump(msg));
            msg.readerIndex(index);
            ctx.fireChannelRead(msg.retain());
        }
    }

    @RequiredArgsConstructor
    private static class BungeeOutPacketHandler extends MessageToByteEncoder<ByteBuf> {
        private final boolean player;

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
            int index = msg.readerIndex();
            System.out.println((player ? "Bungee -> Player" : "Bungee -> Server") + ":\n" + ByteBufUtil.prettyHexDump(msg));
            msg.readerIndex(index);
            out.writeBytes(msg);
        }
    }
}
