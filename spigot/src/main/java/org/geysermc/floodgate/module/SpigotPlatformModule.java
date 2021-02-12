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

package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.inject.spigot.SpigotInjector;
import org.geysermc.floodgate.listener.SpigotListenerRegistration;
import org.geysermc.floodgate.logger.JavaUtilFloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.SpigotPluginMessageRegistration;
import org.geysermc.floodgate.pluginmessage.SpigotPluginMessageUtils;
import org.geysermc.floodgate.pluginmessage.SpigotSkinApplier;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.LanguageManager;
import org.geysermc.floodgate.util.SpigotCommandUtil;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

@RequiredArgsConstructor
public final class SpigotPlatformModule extends AbstractModule {
    private final SpigotPlugin plugin;

    @Provides
    @Singleton
    public JavaPlugin javaPlugin() {
        return plugin;
    }

    @Provides
    @Singleton
    public FloodgateLogger floodgateLogger(LanguageManager languageManager) {
        return new JavaUtilFloodgateLogger(plugin.getLogger(), languageManager);
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            SpigotVersionSpecificMethods versionSpecificMethods,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new SpigotCommandUtil(plugin.getServer(), api, versionSpecificMethods, plugin,
                logger, languageManager);
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
    @Singleton
    public CommonPlatformInjector platformInjector() {
        return new SpigotInjector();
    }

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
    public SkinApplier skinApplier(SpigotVersionSpecificMethods versionSpecificMethods) {
        return new SpigotSkinApplier(versionSpecificMethods, plugin);
    }

    @Provides
    @Singleton
    public SpigotVersionSpecificMethods versionSpecificMethods() {
        return new SpigotVersionSpecificMethods(plugin);
    }
}
