/*
 * Copyright (c) 2021-2022 Minekube. https://minekube.com
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
 * @author Minekube
 * @link https://github.com/minekube/connect-java
 */

package com.minekube.connect.tunnel.p2p;

import com.google.rpc.Code;
import com.google.rpc.Status;
import com.minekube.connect.tunnel.Tunneler;
import com.minekube.connect.watch.SessionProposal;
import io.libp2p.core.Stream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionAccepted;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionOffer;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionRejected;
import minekube.connect.v1alpha1.ConnectLibp2P.SessionResponse;

final class Libp2pSessionResponder {
    static final String PROTOCOL_ID = "/minekube/connect/session/1.0.0";

    private final Starter starter;

	Libp2pSessionResponder(Starter starter) {
		this.starter = Objects.requireNonNull(starter, "starter");
	}

	void install(Stream stream) {
		P2PFrameDecoder<SessionOffer> decoder = new P2PFrameDecoder<>(
				SessionOffer.parser(),
				P2PFrameCodec.MAX_CONTROL_FRAME_SIZE);
		stream.pushHandler(decoder);
		stream.pushHandler(new ControlHandler(stream, decoder));
	}

	void handleOffer(Stream stream, SessionOffer offer) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(offer, "offer");
        SessionProposal proposal = new SessionProposal(
                NativeSessionMapper.toWatchSession(offer),
                reason -> {
                    writeResponse(stream, SessionResponse.newBuilder()
                            .setSessionId(offer.getSessionId())
                            .setRejected(SessionRejected.newBuilder().setReason(reason))
                            .build());
                    stream.close();
                });
        Tunneler tunneler = new Tunneler(new SameStreamTunnelTransport(stream, sessionId ->
                writeResponse(stream, SessionResponse.newBuilder()
                        .setSessionId(sessionId)
                        .setAccepted(SessionAccepted.newBuilder().setSameStreamData(true))
                        .build())));
        try {
            starter.start(proposal, tunneler);
        } catch (RuntimeException e) {
            proposal.reject(Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage() == null ? e.toString() : e.getMessage())
                    .build());
        }
    }

    private static void writeResponse(Stream stream, SessionResponse response) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            P2PFrameCodec.write(out, response);
            stream.writeAndFlush(Unpooled.wrappedBuffer(out.toByteArray()));
        } catch (IOException e) {
            throw new IllegalStateException("encode native session response", e);
        }
    }

	interface Starter {
		void start(SessionProposal proposal, Tunneler tunneler);
	}

	private final class ControlHandler extends SimpleChannelInboundHandler<SessionOffer> {
		private final Stream stream;
		private final P2PFrameDecoder<SessionOffer> decoder;

		private ControlHandler(Stream stream, P2PFrameDecoder<SessionOffer> decoder) {
			this.stream = stream;
			this.decoder = decoder;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, SessionOffer offer) {
			ctx.pipeline().remove(this);
			ctx.pipeline().remove(decoder);
			handleOffer(stream, offer);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			stream.close();
			ctx.close();
		}
	}
}
