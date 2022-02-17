package com.minekube.connect.tunnel;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.minekube.connect.tunnel.TunnelConn.Handler;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import minekube.connect.v1alpha1.TunnelServiceGrpc;
import minekube.connect.v1alpha1.TunnelServiceGrpc.TunnelServiceStub;
import minekube.connect.v1alpha1.TunnelServiceOuterClass.TunnelRequest;
import minekube.connect.v1alpha1.TunnelServiceOuterClass.TunnelResponse;

public class Tunneler implements Closeable {

    private final ConcurrentMap<String, ManagedChannel> channelsByAddr = Maps.newConcurrentMap();

    public ManagedChannel channel(String tunnelServiceAddr) {
        return channelsByAddr.computeIfAbsent(tunnelServiceAddr, addr -> {
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(addr);
            if (!addr.startsWith("https://")) {
                builder.usePlaintext();
            }
            return builder.build();
        });
    }

    public TunnelConn tunnel(final String tunnelServiceAddr, String sessionId, Handler handler) {
        Preconditions.checkArgument(!tunnelServiceAddr.isEmpty(),
                "tunnelServiceAddr must not be empty");

        Metadata.Key<String> s = Metadata.Key.of("Connect-Session",
                Metadata.ASCII_STRING_MARSHALLER);
        Metadata metadata = new Metadata();
        metadata.put(s, sessionId);
        TunnelServiceStub asyncStub = TunnelServiceGrpc.newStub(channel(tunnelServiceAddr))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        AtomicBoolean closeHandlerOnce = new AtomicBoolean();
        Runnable handlerOnClose = () -> {
            if (closeHandlerOnce.compareAndSet(false, true)) {
                handler.onClose();
            }
        };

        StreamObserver<TunnelRequest> writeStream = asyncStub.tunnel(
                new StreamObserver<TunnelResponse>() {
                    @Override
                    public void onNext(final TunnelResponse value) {
                        handler.onReceive(value.getData().toByteArray());
                    }

                    @Override
                    public void onError(final Throwable t) {
                        handler.onError(t);
                        handlerOnClose.run();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("read completed");
                        handlerOnClose.run();
                    }
                });
        return new TunnelConn() {
            @Override
            public void write(TunnelRequest req) {
                writeStream.onNext(req);
            }

            @Override
            public void close(Throwable t) {
                if (t == null) {
                    writeStream.onCompleted();
                } else {
                    writeStream.onError(t);
                }
                handlerOnClose.run();
            }
        };
    }

    @Override
    public void close() {
        // disconnects all tunneled players instantaneously
        channelsByAddr.forEach((addr, channel) -> {
            channel.shutdownNow();
            channelsByAddr.remove(addr);
        });
    }
}
