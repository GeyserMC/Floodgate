package com.minekube.connect.watch;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicReference;
import minekube.connect.v1alpha1.WatchServiceGrpc;
import minekube.connect.v1alpha1.WatchServiceGrpc.WatchServiceStub;
import minekube.connect.v1alpha1.WatchServiceOuterClass.SessionRejection;
import minekube.connect.v1alpha1.WatchServiceOuterClass.WatchRequest;
import minekube.connect.v1alpha1.WatchServiceOuterClass.WatchResponse;

public class WatchClient {

    private final WatchServiceStub asyncStub;

    public WatchClient(WatchServiceStub asyncStub) {
        this.asyncStub = asyncStub;
    }

    public WatchClient(ManagedChannel channel) {
        this.asyncStub = WatchServiceGrpc.newStub(channel);
    }

    public void watch(Watcher watcher) {
        AtomicReference<StreamObserver<WatchRequest>> reqStream = new AtomicReference<>();
        StreamObserver<WatchResponse> resStream = new StreamObserver<WatchResponse>() {
            @Override
            public void onNext(final WatchResponse res) {
                if (!res.hasSession()) {
                    return;
                }
                SessionProposal prop = new SessionProposal(
                        res.getSession(),
                        reason -> reqStream.get().onNext(WatchRequest.newBuilder()
                                .setSessionRejection(SessionRejection.newBuilder()
                                        .setId(res.getSession().getId())
                                        .setReason(reason)
                                        .build())
                                .build())
                );
                watcher.onProposal(prop);
            }

            @Override
            public void onError(final Throwable t) {
                watcher.onError(t);
            }

            @Override
            public void onCompleted() {
                watcher.onCompleted();
            }
        };
        reqStream.set(asyncStub.watch(resStream));
    }

}
