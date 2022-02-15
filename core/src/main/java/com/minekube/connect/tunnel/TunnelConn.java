package com.minekube.connect.tunnel;

import com.google.protobuf.ByteString;
import java.io.Closeable;
import minekube.connect.v1alpha1.TunnelServiceOuterClass.TunnelRequest;

public abstract class TunnelConn implements Closeable {

    public interface Handler {
        void onReceive(byte[] data);

        void onError(Throwable t);

        default void onClose() {
        }
    }

    public abstract void write(TunnelRequest req);

    public void write(byte[] data) {
        write(ByteString.copyFrom(data));
    }

    public void write(ByteString data) {
        write(TunnelRequest.newBuilder().setData(data).build());
    }

    public abstract void close(Throwable t);

    @Override
    public abstract void close();
}
