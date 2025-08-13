/*
 * Wrapper around IoHandler that delegates between LocalIoHandler and native handler
 * Based on Geyser's approach for handling LocalServerChannel with BungeeCord.
 */
package com.minekube.connect.inject.bungee;

import io.netty.channel.IoHandle;
import io.netty.channel.IoHandler;
import io.netty.channel.IoHandlerContext;
import io.netty.channel.IoRegistration;

public class ConnectIoHandlerWrapper implements IoHandler {
    private final IoHandler localHandler;
    private final IoHandler nativeHandler;
    private final IoHandlerContextWrapper contextWrapper = new IoHandlerContextWrapper();

    public ConnectIoHandlerWrapper(IoHandler localHandler, IoHandler nativeHandler) {
        this.localHandler = localHandler;
        this.nativeHandler = nativeHandler;
    }

    @Override
    public void initialize() {
        localHandler.initialize();
        nativeHandler.initialize();
    }

    @Override
    public int run(IoHandlerContext context) {
        contextWrapper.base = context;
        localHandler.run(contextWrapper);
        return nativeHandler.run(context);
    }

    private static class IoHandlerContextWrapper implements IoHandlerContext {
        private IoHandlerContext base;

        @Override
        public boolean canBlock() {
            return false;
        }

        @Override
        public long delayNanos(long currentTimeNanos) {
            return base.delayNanos(currentTimeNanos);
        }

        @Override
        public long deadlineNanos() {
            return base.deadlineNanos();
        }
    }

    @Override
    public void prepareToDestroy() {
        localHandler.prepareToDestroy();
        nativeHandler.prepareToDestroy();
    }

    @Override
    public void destroy() {
        localHandler.destroy();
        nativeHandler.destroy();
    }

    private static final Class<? extends IoHandle> LOCAL_HANDLER_CLASS;

    static {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends IoHandle> clazz = (Class<? extends IoHandle>) Class.forName("io.netty.channel.local.LocalIoHandle");
            LOCAL_HANDLER_CLASS = clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IoRegistration register(IoHandle handle) throws Exception {
        if (LOCAL_HANDLER_CLASS.isAssignableFrom(handle.getClass())) {
            return this.localHandler.register(handle);
        }
        return this.nativeHandler.register(handle);
    }

    @Override
    public void wakeup() {
        localHandler.wakeup();
        nativeHandler.wakeup();
    }

    @Override
    public boolean isCompatible(Class<? extends IoHandle> handleType) {
        return localHandler.isCompatible(handleType) || nativeHandler.isCompatible(handleType);
    }
}


