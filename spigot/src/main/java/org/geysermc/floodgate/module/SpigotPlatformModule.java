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
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.spigot.SpigotInjector;
import org.geysermc.floodgate.listener.SpigotListenerRegistration;
import org.geysermc.floodgate.core.logger.JavaUtilFloodgateLogger;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.SpigotPluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.SpigotPluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.SpigotSkinApplier;
import org.geysermc.floodgate.core.skin.SkinApplier;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.util.SpigotCommandUtil;
import org.geysermc.floodgate.util.SpigotPlatformUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

@RequiredArgsConstructor
public final class SpigotPlatformModule extends AbstractModule {
    private final SpigotPlugin plugin;

    @Override
    protected void configure() {
        bind(SpigotPlugin.class).toInstance(plugin);
        bind(PlatformUtils.class).to(SpigotPlatformUtils.class);
        bind(CommonPlatformInjector.class).to(SpigotInjector.class);
        bind(Logger.class).annotatedWith(Names.named("logger")).toInstance(plugin.getLogger());
        bind(FloodgateLogger.class).to(JavaUtilFloodgateLogger.class);
        bind(SkinApplier.class).to(SpigotSkinApplier.class);
    }

    @Provides
    @Singleton
    public JavaPlugin javaPlugin() {
        return plugin;
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            SpigotVersionSpecificMethods versionSpecificMethods,
            LanguageManager languageManager) {
        return new SpigotCommandUtil(
                languageManager, plugin.getServer(), api, versionSpecificMethods);
    }

    @Provides
    @Singleton
    public ListenerRegistration<Listener> listenerRegistration() {
        return new SpigotListenerRegistration(plugin);
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Named("packetEncoder")
    public String packetEncoder() {
        return "encoder";
    }

    @Provides
    @Named("packetDecoder")
    public String packetDecoder() {
        return "decoder";
    }

    @Provides
    @Named("packetHandler")
    public String packetHandler() {
        return "packet_handler";
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Spigot";
    }

    /*
    Others
     */

    @Provides
    @Singleton
    public PluginMessageUtils pluginMessageUtils() {
        return new SpigotPluginMessageUtils(plugin);
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegister() {
        return new SpigotPluginMessageRegistration(plugin);
    }

    @Provides
    @Singleton
    public SpigotVersionSpecificMethods versionSpecificMethods() {
        return new SpigotVersionSpecificMethods(plugin);
    }
}
