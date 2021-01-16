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

package org.geysermc.floodgate.inject.spigot;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.ReflectionUtils;

@RequiredArgsConstructor
public final class SpigotInjector extends CommonPlatformInjector {
    private final Set<Channel> injectedClients = new HashSet<>();

    private Object serverConnection;
    private String injectedFieldName;

    @Getter private boolean injected;

    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public boolean inject() throws Exception {
        if (isInjected()) {
            return true;
        }

        if (getServerConnection() != null) {
            for (Field field : serverConnection.getClass().getDeclaredFields()) {
                if (field.getType() == List.class) {
                    field.setAccessible(true);

                    ParameterizedType parameterType = ((ParameterizedType) field.getGenericType());
                    Type listType = parameterType.getActualTypeArguments()[0];

                    // the list we search has ChannelFuture as type
                    if (listType != ChannelFuture.class) {
                        continue;
                    }

                    injectedFieldName = field.getName();
                    List<?> newList = new CustomList((List<?>) field.get(serverConnection)) {
                        @Override
                        public void onAdd(Object object) {
                            try {
                                injectClient((ChannelFuture) object);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    };

                    // inject existing
                    synchronized (newList) {
                        for (Object object : newList) {
                            try {
                                injectClient((ChannelFuture) object);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    }

                    field.set(serverConnection, newList);
                    injected = true;
                    return true;
                }
            }
        }
        return false;
    }

    public void injectClient(ChannelFuture future) {
        future.channel().pipeline().addFirst("floodgate-init", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                super.channelRead(ctx, msg);

                Channel channel = (Channel) msg;
                channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        injectAddonsCall(channel, false);
                        injectedClients.add(channel);
                    }
                });
            }
        });
    }

    @Override
    public boolean removeInjection() throws Exception {
        if (!isInjected()) {
            return true;
        }

        // remove injection from clients
        for (Channel channel : injectedClients) {
            removeAddonsCall(channel);
        }
        injectedClients.clear();

        // and change the list back to the original
        Object serverConnection = getServerConnection();
        if (serverConnection != null) {
            Field field = ReflectionUtils.getField(serverConnection.getClass(), injectedFieldName);
            List<?> list = (List<?>) ReflectionUtils.getValue(serverConnection, field);

            if (list instanceof CustomList) {
                CustomList customList = (CustomList) list;
                ReflectionUtils.setValue(serverConnection, field, customList.getOriginalList());
                customList.clear();
                customList.addAll(list);
            }
        }

        injected = false;
        return true;
    }

    public Object getServerConnection() throws IllegalAccessException, InvocationTargetException {
        if (serverConnection != null) {
            return serverConnection;
        }
        Class<?> minecraftServer = ReflectionUtils.getPrefixedClass("MinecraftServer");
        assert minecraftServer != null;

        Object minecraftServerInstance = ReflectionUtils.invokeStatic(minecraftServer, "getServer");
        for (Method method : minecraftServer.getDeclaredMethods()) {
            if ("ServerConnection".equals(method.getReturnType().getSimpleName())) {
                if (method.getParameterTypes().length == 0) {
                    serverConnection = method.invoke(minecraftServerInstance);
                }
            }
        }

        return serverConnection;
    }
}
