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

package com.minekube.connect.module;

import static com.minekube.connect.util.ReflectionUtils.getClassSilently;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.minekube.connect.SpigotPlugin;
import com.minekube.connect.api.ConnectApi;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.inject.CommonPlatformInjector;
import com.minekube.connect.inject.spigot.SpigotInjector;
import com.minekube.connect.listener.SpigotListenerRegistration;
import com.minekube.connect.logger.JavaUtilConnectLogger;
import com.minekube.connect.platform.command.CommandUtil;
import com.minekube.connect.platform.listener.ListenerRegistration;
import com.minekube.connect.pluginmessage.SpigotSkinApplier;
import com.minekube.connect.skin.SkinApplier;
import com.minekube.connect.util.LanguageManager;
import com.minekube.connect.util.SpigotCommandUtil;
import com.minekube.connect.util.SpigotVersionSpecificMethods;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

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
    public ConnectLogger logger(LanguageManager languageManager) {
        return new JavaUtilConnectLogger(plugin.getLogger(), languageManager);
    }

    /*
    Commands / Listeners
     */

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            ConnectApi api,
            SpigotVersionSpecificMethods versionSpecificMethods,
            LanguageManager languageManager) {
        return new SpigotCommandUtil(
                languageManager, plugin.getServer(), api, versionSpecificMethods, plugin);
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
    public CommonPlatformInjector platformInjector(ConnectLogger logger) {
        final String VIAVERSION_DOWNLOAD_URL = "https://ci.viaversion.com/job/ViaVersion/";
        boolean isViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
        if (isViaVersion) {
            // Ensure that we have the latest 4.0.0 changes and not an older ViaVersion version
            if (getClassSilently("com.viaversion.viaversion.api.ViaManager") == null) {
                logger.warn("The plugin version of ViaVersion is too old, " +
                                "please install the latest release from {}",
                        VIAVERSION_DOWNLOAD_URL);
                isViaVersion = false;
            }
        }

        return new SpigotInjector(logger, isViaVersion);
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

//    @Provides
//    @Singleton
//    public PluginMessageUtils pluginMessageUtils() {
//        return new SpigotPluginMessageUtils(plugin);
//    }
//
//    @Provides
//    @Singleton
//    public PluginMessageRegistration pluginMessageRegister() {
//        return new SpigotPluginMessageRegistration(plugin);
//    }

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
