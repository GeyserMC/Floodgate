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

package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.netty.util.AttributeKey;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import org.geysermc.configutils.file.template.ResourceTemplateReader;
import org.geysermc.configutils.file.template.TemplateReader;
import org.geysermc.event.PostOrder;
import org.geysermc.floodgate.addon.data.HandshakeHandlersImpl;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.event.FloodgateEventBus;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ConfigLoader;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.crypto.KeyProducer;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.lifecycle.ShutdownEvent;
import org.geysermc.floodgate.event.util.ListenerAnnotationMatcher;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.link.PlayerLinkHolder;
import org.geysermc.floodgate.packet.PacketHandlersImpl;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.skin.SkinUploadManager;
import org.geysermc.floodgate.util.Constants;
import org.geysermc.floodgate.util.HttpClient;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public class CommonModule extends AbstractModule {
    private final EventBus eventBus = new EventBus();
    private final Path dataDirectory;
    private final TemplateReader reader;

    public CommonModule(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.reader = ResourceTemplateReader.of(ConfigLoader.class);
    }

    @Override
    protected void configure() {
        bind(EventBus.class).toInstance(eventBus);
        bind(FloodgateEventBus.class).to(EventBus.class);
        // register every class that has the Listener annotation
        bindListener(new ListenerAnnotationMatcher(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                encounter.register((InjectionListener<I>) eventBus::register);
            }
        });

        ExecutorService commonPool = Executors.newCachedThreadPool();
        ScheduledExecutorService commonScheduledPool = Executors.newSingleThreadScheduledExecutor();

        eventBus.subscribe(ShutdownEvent.class, ignored -> {
            commonPool.shutdown();
            commonScheduledPool.shutdown();
        }, PostOrder.LAST);

        bind(ExecutorService.class)
                .annotatedWith(Names.named("commonPool"))
                .toInstance(commonPool);
        bind(ScheduledExecutorService.class)
                .annotatedWith(Names.named("commonScheduledPool"))
                .toInstance(commonScheduledPool);

        bind(HttpClient.class).in(Singleton.class);

        bind(FloodgateApi.class).to(SimpleFloodgateApi.class);
        bind(PlatformInjector.class).to(CommonPlatformInjector.class);

        bind(HandshakeHandlers.class).to(HandshakeHandlersImpl.class);
        bind(HandshakeHandlersImpl.class).in(Singleton.class);

        bind(PacketHandlers.class).to(PacketHandlersImpl.class);
        bind(PacketHandlersImpl.class).asEagerSingleton();

        install(new AutoBindModule());
    }

    @Provides
    @Singleton
    public FloodgateConfig floodgateConfig(ConfigLoader configLoader) {
        return configLoader.load();
    }

    @Provides
    @Singleton
    public PlayerLink playerLink(PlayerLinkHolder linkLoader) {
        return linkLoader.load();
    }

    @Provides
    @Singleton
    public KeyProducer keyProducer() {
        return new AesKeyProducer();
    }

    @Provides
    @Singleton
    public FloodgateCipher cipher() {
        return new AesCipher(new Base64Topping());
    }

    @Provides
    @Singleton
    @Named("dataDirectory")
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Provides
    @Singleton
    public ConfigLoader configLoader(
            @Named("configClass") Class<? extends FloodgateConfig> configClass,
            KeyProducer producer,
            FloodgateCipher cipher) {
        return new ConfigLoader(dataDirectory, configClass, producer, cipher, reader);
    }

    @Provides
    @Singleton
    public FloodgateHandshakeHandler handshakeHandler(
            HandshakeHandlersImpl handshakeHandlers,
            SimpleFloodgateApi api,
            FloodgateCipher cipher,
            FloodgateConfig config,
            SkinUploadManager skinUploadManager,
            @Named("playerAttribute") AttributeKey<FloodgatePlayer> playerAttribute,
            FloodgateLogger logger,
            LanguageManager languageManager) {

        return new FloodgateHandshakeHandler(handshakeHandlers, api, cipher, config,
                skinUploadManager, playerAttribute, logger, languageManager);
    }

    @Provides
    @Singleton
    public PluginMessageManager pluginMessageManager() {
        return new PluginMessageManager();
    }

    @Provides
    @Singleton
    @Named("gitBranch")
    public String gitBranch() {
        return Constants.GIT_BRANCH;
    }

    @Provides
    @Singleton
    @Named("buildNumber")
    public int buildNumber() {
        return Constants.BUILD_NUMBER;
    }

    @Provides
    @Singleton
    @Named("kickMessageAttribute")
    public AttributeKey<String> kickMessageAttribute() {
        return AttributeKey.valueOf("floodgate-kick-message");
    }

    @Provides
    @Singleton
    @Named("playerAttribute")
    public AttributeKey<FloodgatePlayer> playerAttribute() {
        return AttributeKey.valueOf("floodgate-player");
    }
}
