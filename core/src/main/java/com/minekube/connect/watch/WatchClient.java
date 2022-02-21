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
