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

package com.minekube.connect.inject.spigot;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.network.netty.LocalServerChannelWrapper;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.util.ClassNames;
import com.minekube.connect.util.ReflectionUtils;
import com.viaversion.viaversion.bukkit.handlers.BukkitChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@RequiredArgsConstructor
public final class SpigotInjector extends CommonPlatformInjector {
    private final ConnectLogger logger;
    /**
     * Used to determine if ViaVersion is set up to a state where Connect players will fail at
     * joining if injection is enabled
     */
    private final boolean isViaVersion;
    /**
     * Used to uninject ourselves on shutdown.
     */
    private CustomList allServerChannels;

    private Object serverConnection;

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

                    CustomList newList = new CustomList((List<?>) field.get(serverConnection)) {
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

                    allServerChannels = newList;
                    initializeLocalChannel();
                    field.set(serverConnection, newList);

                    injected = true;
                    return true;
                }
            }
        }
        return false;
    }

    public void injectClient(ChannelFuture future) {
        future.channel().pipeline().addFirst("connect-init", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                super.channelRead(ctx, msg);

                Channel channel = (Channel) msg;
                // only need to inject if is a local session & auth passthrough is disabled
                LocalSession.context(channel)
                        .filter(context -> !context.getPlayer().getAuth().isPassthrough())
                        .ifPresent(
                                $ -> channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                                    @Override
                                    protected void initChannel(Channel channel) {
                                        injectAddonsCall(channel, false);
                                        addInjectedClient(channel);
                                    }
                                }));
            }
        });
    }

    public Object getServerConnection() throws IllegalAccessException, InvocationTargetException {
        if (serverConnection != null) {
            return serverConnection;
        }
        Class<?> minecraftServer = ClassNames.MINECRAFT_SERVER;

        // method by CraftBukkit to get the instance of the MinecraftServer
        Object minecraftServerInstance = ReflectionUtils.invokeStatic(minecraftServer, "getServer");

        for (Method method : minecraftServer.getDeclaredMethods()) {
            if (ClassNames.SERVER_CONNECTION.equals(method.getReturnType())) {
                // making sure that it's a getter
                if (method.getParameterTypes().length == 0) {
                    serverConnection = method.invoke(minecraftServerInstance);
                }
            }
        }

        return serverConnection;
    }

    // Start of logic from GeyserMC
    // https://github.com/GeyserMC/Geyser/blob/31fd57a58d19829071859ef292fee706873d31fb/bootstrap/spigot/src/main/java/org/geysermc/geyser/platform/spigot/GeyserSpigotInjector.java#L62

    @SuppressWarnings("unchecked")
    private void initializeLocalChannel() throws Exception {
        // Find the channel that Minecraft uses to listen to connections
        ChannelFuture listeningChannel = null;
        for (Object o : allServerChannels) {
            listeningChannel = (ChannelFuture) o;
            break;
        }
        if (listeningChannel == null) {
            throw new RuntimeException("Unable to find listening channel!");
        }

        // Making this a function prevents childHandler from being treated as a non-final variable
        ChannelInitializer<Channel> childHandler = getChildHandler(listeningChannel);
        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = childHandler.getClass().getDeclaredMethod("initChannel",
                Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        initChannel.invoke(childHandler, ch);
                    }
                })
                // Set to MAX_PRIORITY as MultithreadEventLoopGroup#newDefaultThreadFactory which DefaultEventLoopGroup implements does by default
                .group(new DefaultEventLoopGroup(0,
                        new DefaultThreadFactory("Geyser Spigot connection thread",
                                Thread.MAX_PRIORITY)))
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();
        // We don't need to add to the list, but plugins like ProtocolSupport and ProtocolLib that add to the main pipeline
        // will work when we add to the list.
        allServerChannels.add(channelFuture);
        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();

        workAroundWeirdBug();
    }

    @SuppressWarnings("unchecked")
    private ChannelInitializer<Channel> getChildHandler(ChannelFuture listeningChannel) {
        List<String> names = listeningChannel.channel().pipeline().names();
        ChannelInitializer<Channel> childHandler = null;
        for (String name : names) {
            ChannelHandler handler = listeningChannel.channel().pipeline().get(name);
            try {
                Field childHandlerField = handler.getClass().getDeclaredField("childHandler");
                childHandlerField.setAccessible(true);
                childHandler = (ChannelInitializer<Channel>) childHandlerField.get(handler);
                // ViaVersion non-Paper-injector workaround so we aren't double-injecting
                if (isViaVersion && childHandler instanceof BukkitChannelInitializer) {
                    childHandler = ((BukkitChannelInitializer) childHandler).getOriginal();
                }
                break;
            } catch (Exception e) {
                if (logger.isDebug()) {
                    logger.debug("The handler " + name +
                            " isn't a ChannelInitializer. THIS ERROR IS SAFE TO IGNORE!");
                    e.printStackTrace();
                }
            }
        }
        if (childHandler == null) {
            throw new RuntimeException();
        }
        return childHandler;
    }

    @Override
    public void shutdown() {
        if (this.allServerChannels != null) {
            this.allServerChannels.remove(this.localChannel);
            this.allServerChannels = null;
        }
        super.shutdown();
    }

    /**
     * Work around an odd bug where the first connection might not initialize all channel handlers
     * on the main pipeline - connecting down as the first connection fixes this. For the future, if
     * someone wants to properly fix this - as of December 28, 2021, it happens on
     * 1.16.5/1.17.1/1.18.1 EXCEPT Spigot 1.16.5
     */
    private void workAroundWeirdBug() {
        // connect to local server and close
        connectAndClose(); // that's enough
    }

    // End of logic from GeyserMC

    private void connectAndClose() {
        new Bootstrap()
                .remoteAddress(serverSocketAddress)
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(@NonNull Channel ch) {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(@NonNull ChannelHandlerContext ctx) {
                                ctx.close();
                            }
                        });
                    }
                })
                .group(new DefaultEventLoopGroup(0,
                        new DefaultThreadFactory("Geyser Spigot workAroundWeirdBug thread",
                                Thread.MAX_PRIORITY)))
                .connect()
                .syncUninterruptibly();
    }

}
