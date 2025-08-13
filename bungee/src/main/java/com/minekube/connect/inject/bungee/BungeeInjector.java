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

package com.minekube.connect.inject.bungee;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.network.netty.LocalServerChannelWrapper;
import com.minekube.connect.network.netty.LocalSession;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoEventLoop;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalIoHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ProxyServer.Unsafe;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.channel.BungeeChannelInitializer;

@RequiredArgsConstructor
public final class BungeeInjector extends CommonPlatformInjector implements Listener {

    private final ConnectLogger logger;
    private final ProxyServer proxy;
    private final Plugin plugin;
    @Getter private boolean injected;


    @Override
    public boolean inject() {
        try {
            // Can everyone just switch to Velocity please :)
            // :( ~BungeeCord Collaborator

            Unsafe unsafe = ProxyServer.getInstance().unsafe();
            BungeeChannelInitializer frontend = unsafe.getFrontendChannelInitializer();
            unsafe.setFrontendChannelInitializer(BungeeChannelInitializer.create(channel -> {
                if (!frontend.getChannelAcceptor().accept(channel)) {
                    return false;
                }
                injectClient(channel, true);
                return true;
            }));

            BungeeChannelInitializer backend = unsafe.getBackendChannelInitializer();
            unsafe.setBackendChannelInitializer(BungeeChannelInitializer.create(channel -> {
                if (!backend.getChannelAcceptor().accept(channel)) {
                    return false;
                }
                injectClient(channel, false);
                return true;
            }));

            initializeLocalChannel0();
            injected = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void initializeLocalChannel() {
//        if (!bootstrap.getGeyserConfig().isUseDirectConnection()) { TODO
//            bootstrap.getGeyserLogger().debug("Disabling direct injection!");
//            return;
//        }

        if (this.localChannel != null) {
            logger.warn(
                    "Connect attempted to inject into the server connection handler twice! Please ensure you aren't using /reload or any plugin that (re)loads Connect after the server has started.");
            return;
        }

        try {
            initializeLocalChannel0();
            logger.debug("Local injection succeeded!");
        } catch (Exception e) {
            e.printStackTrace();
            // If the injector partially worked, undo it
            shutdown();
        }
    }

    // Start of logic from GeyserMC
    // source https://github.com/GeyserMC/Geyser/blob/252348ebd8fc0097a415dc4cbb165ae101f42fd5/bootstrap/bungeecord/src/main/java/org/geysermc/geyser/platform/bungeecord/GeyserBungeeInjector.java#L69

    /**
     * Set as a variable, so it is only set after the proxy has finished initializing
     */
    private ChannelInitializer<Channel> channelInitializer = null;
    private Set<Channel> bungeeChannels = null;
    private boolean eventRegistered = false;

    @SuppressWarnings("unchecked")
    private void initializeLocalChannel0() throws Exception {
        // TODO - allow Geyser to specify its own listener info properties
        if (proxy.getConfig().getListeners().size() != 1) {
            throw new UnsupportedOperationException(
                    "Connect does not currently support multiple listeners with injection! " +
                            "Please reach out to us on our Discord at https://minekube.com/discord so we can hear feedback on your setup.");
        }

        try {
            ProxyServer.class.getMethod("unsafe");
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("You're using an outdated version of BungeeCord - please update. Thank you!");
        }

        ListenerInfo listenerInfo = proxy.getConfig().getListeners().stream().findFirst().orElseThrow(
                IllegalStateException::new);

        Class<? extends ProxyServer> proxyClass = proxy.getClass();
        // Using the specified EventLoop is required, or else an error will be thrown
        MultiThreadIoEventLoopGroup bungeeWorkerGroup;
        try {
            bungeeWorkerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("eventLoops").get(proxy);
            logger.debug("BungeeCord event loop style detected.");
        } catch (NoSuchFieldException e) {
            // Waterfall uses two separate event loops
            // https://github.com/PaperMC/Waterfall/blob/fea7ec356dba6c6ac28819ff11be604af6eb484e/BungeeCord-Patches/0022-Use-a-worker-and-a-boss-event-loop-group.patch
            bungeeWorkerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("workerEventLoopGroup").get(proxy);
            logger.debug("Waterfall event loop style detected.");
        }

        // Use Geyser's approach: create wrapper event loops that delegate properly
        final IoEventLoopGroup finalWorkerGroup = bungeeWorkerGroup;
        IoHandlerFactory localFactory = LocalIoHandler.newFactory();
        IoHandlerFactory nativeFactory = io.netty.channel.nio.NioIoHandler.newFactory();

        IoHandlerFactory wrapperFactory = ioExecutor -> new ConnectIoHandlerWrapper(
                localFactory.newHandler(ioExecutor),
                nativeFactory.newHandler(ioExecutor)
        );

        EventLoopGroup wrapperGroup = new MultiThreadIoEventLoopGroup(localFactory) {
            @Override
            protected ThreadFactory newDefaultThreadFactory() {
                return new DefaultThreadFactory("Connect BungeeCord Worker Group");
            }

            @Override
            protected IoEventLoop newChild(Executor executor, IoHandlerFactory ioHandlerFactory, Object... args) {
                // Use SingleThreadIoEventLoop with the BungeeCord worker group and wrapperFactory
                // Ensures LocalChannels use LocalIoHandler while native channels use NIO
                return new SingleThreadIoEventLoop(finalWorkerGroup, executor, wrapperFactory);
            }
        };

        // Is currently just AttributeKey.valueOf("ListerInfo") but we might as well copy the value itself.
        AttributeKey<ListenerInfo> listener = PipelineUtils.LISTENER;
        listenerInfo = new ListenerInfo(
                listenerInfo.getSocketAddress(),
                listenerInfo.getMotd(),
                listenerInfo.getMaxPlayers(),
                listenerInfo.getTabListSize(),
                listenerInfo.getServerPriority(),
                listenerInfo.isForceDefault(),
                listenerInfo.getForcedHosts(),
                listenerInfo.getTabListType(),
                listenerInfo.isSetLocalAddress(),
                listenerInfo.isPingPassthrough(),
                listenerInfo.getQueryPort(),
                listenerInfo.isQueryEnabled(),
                false
                // If Geyser is expecting HAProxy, so should the Bungee end
        );

        // The field that stores all listeners in BungeeCord
        // As of https://github.com/ViaVersion/ViaVersion/pull/2698 ViaVersion adds a wrapper to this field to
        // add its connections
        Field listenerField = proxyClass.getDeclaredField("listeners");
        listenerField.setAccessible(true);
        bungeeChannels = (Set<Channel>) listenerField.get(proxy);

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel",
                Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (proxy.getConfig().getServers() == null) {
                            // Proxy hasn't finished loading all plugins - it loads the config after all plugins
                            // Probably doesn't need to be translatable?
                            logger.info(
                                    "Disconnecting player as Bungee has not finished loading");
                            ch.close();
                            return;
                        }

                        if (channelInitializer == null) {
                            // Proxy has finished initializing; we can safely grab this variable without fear of plugins modifying it
                            // (Older versions of ViaVersion replace this to inject)
                            channelInitializer = proxy.unsafe().getFrontendChannelInitializer().getChannelInitializer();
                        }
                        initChannel.invoke(channelInitializer, ch);
                    }
                })
                .childAttr(listener, listenerInfo)
                .group(wrapperGroup)
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.bungeeChannels.add(this.localChannel.channel());
        this.serverSocketAddress = channelFuture.channel().localAddress();

        if (!this.eventRegistered) {
            // Register reload listener
            this.proxy.getPluginManager().registerListener(this.plugin, this);
            this.eventRegistered = true;
        }

        // Set the platform event loop group for LocalSession to use
        // This ensures LocalSession connections use BungeeCord's event loops
        LocalSession.setPlatformEventLoopGroup(wrapperGroup);
        
        // Only affects Waterfall, but there is no sure way to differentiate between a proxy with this patch and a proxy without this patch
        // Patch causing the issue: https://github.com/PaperMC/Waterfall/blob/7e6af4cef64d5d377a6ffd00a534379e6efa94cf/BungeeCord-Patches/0045-Don-t-use-a-bytebuf-for-packet-decoding.patch
        // If native compression is enabled, then this line is tripped up if a heap buffer is sent over in such a situation
        // as a new direct buffer is not created with that patch (HeapByteBufs throw an UnsupportedOperationException here):
        // https://github.com/SpigotMC/BungeeCord/blob/a283aaf724d4c9a815540cd32f3aafaa72df9e05/native/src/main/java/net/md_5/bungee/jni/zlib/NativeZlib.java#L43
        // This issue could be mitigated down the line by preventing Bungee from setting compression
        LocalSession.createDirectByteBufAllocator();
    }

    @Override
    public void shutdown() {
        if (this.localChannel != null && this.bungeeChannels != null) {
            this.bungeeChannels.remove(this.localChannel.channel());
            this.bungeeChannels = null;
        }
        super.shutdown();
    }

    /**
     * The reload process clears the listeners field. Since we need to add to the listeners for
     * maximum compatibility, we also need to re-add and re-enable our listener if a reload is
     * initiated.
     */
    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        this.bungeeChannels = null;
        if (this.localChannel != null) {
            shutdown();
            initializeLocalChannel();
        }
    }

    // End of logic from GeyserMC

    void injectClient(Channel channel, boolean clientToProxy) {
        injectAddonsCall(channel, !clientToProxy);
        addInjectedClient(channel);
    }
}