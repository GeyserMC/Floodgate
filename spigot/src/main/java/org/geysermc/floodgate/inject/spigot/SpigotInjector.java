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

package org.geysermc.floodgate.inject.spigot;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.ReflectionUtils;

@RequiredArgsConstructor
public final class SpigotInjector extends CommonPlatformInjector {
    private Object serverConnection;
    private String injectedFieldName;

    @Getter private boolean injected;

    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void inject() throws Exception {
        if (isInjected()) {
            return;
        }

        Object serverConnection = getServerConnection();
        if (serverConnection == null) {
            throw new RuntimeException("Unable to find server connection");
        }

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
                return;
            }
        }
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
                        addInjectedClient(channel);
                    }
                });
            }
        });
    }

    @Override
    public void removeInjection() {
        if (!isInjected()) {
            return;
        }

        // let's change the list back to the original first
        // so that new connections are not handled through our custom list
        Object serverConnection = getServerConnection();
        if (serverConnection != null) {
            Field field = ReflectionUtils.getField(serverConnection.getClass(), injectedFieldName);

            if (!findAndReplaceCustomList(serverConnection, field)) {
                throw new IllegalStateException(
                        "Unable to remove all references of Floodgate! " +
                        "Reloading will very likely break Floodgate."
                );
            }
        }

        // remove injection from clients
        for (Channel channel : injectedClients()) {
            removeAddonsCall(channel);
        }

        //todo make sure that all references are removed from the channels,
        // except from one AttributeKey with Floodgate player data which could be used
        // after reloading

        injected = false;
    }

    /**
     * Replaces all references of CustomList with the original list.
     * It's entirely possible that e.g. the most recent channel map override isn't Floodgate's
     */
    private boolean findAndReplaceCustomList(Object object, Field possibleListField) {
        Object value = ReflectionUtils.getValue(object, possibleListField);

        if (value == null || value.getClass() == Object.class) {
            return false;
        }

        if (value instanceof CustomList) {
            // all we have to do is replace the list with the original list.
            // the original list is up-to-date, so we don't have to clear/add/whatever anything
            CustomList customList = (CustomList) value;
            ReflectionUtils.setValue(object, possibleListField, customList.getOriginalList());
        }

        boolean found = false;
        for (Field field : value.getClass().getDeclaredFields()) {
            // normally list types don't have a lot of fields, so let's just try all of them
            found |= findAndReplaceCustomList(value, field);
        }

        return found;
    }

    private Object getServerConnection() {
        if (serverConnection != null) {
            return serverConnection;
        }
        Class<?> minecraftServer = ClassNames.MINECRAFT_SERVER;

        // method by CraftBukkit to get the instance of the MinecraftServer
        Object minecraftServerInstance = ReflectionUtils.invokeStatic(minecraftServer, "getServer");

        Method method = ReflectionUtils.getMethodThatReturns(
                minecraftServer, ClassNames.SERVER_CONNECTION, true
        );

        serverConnection = ReflectionUtils.invoke(minecraftServerInstance, method);

        return serverConnection;
    }
}
