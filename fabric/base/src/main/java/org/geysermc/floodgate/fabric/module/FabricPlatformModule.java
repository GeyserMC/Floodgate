/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.fabric.module;

import org.geysermc.floodgate.fabric.inject.fabric.FabricInjector;
import org.geysermc.floodgate.fabric.listener.FabricEventListener;
import org.geysermc.floodgate.fabric.listener.FabricEventRegistration;
import org.geysermc.floodgate.fabric.listener.logger.Log4jFloodgateLogger;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.platform.util.PlatformUtils;
import org.geysermc.floodgate.fabric.pluginmessage.FabricPluginMessageRegistration;
import org.geysermc.floodgate.fabric.pluginmessage.FabricPluginMessageUtils;
import org.geysermc.floodgate.fabric.pluginmessage.FabricSkinApplier;
import org.geysermc.floodgate.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.fabric.util.FabricCommandUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.inject.CommonPlatformInjector;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.fabric.util.FabricPlatformUtils;
import org.geysermc.floodgate.util.LanguageManager;

@RequiredArgsConstructor
public final class FabricPlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlatformUtils.class).to(FabricPlatformUtils.class);
    }

    @Provides
    @Singleton
    public FloodgateLogger floodgateLogger(LanguageManager languageManager) {
        return new Log4jFloodgateLogger(LogManager.getLogger("floodgate"), languageManager);
    }

    @Provides
    @Singleton
    public CommandUtil commandUtil(
            FloodgateApi api,
            FloodgateLogger logger,
            LanguageManager languageManager) {
        return new FabricCommandUtil(languageManager, api, logger);
    }

    @Provides
    @Singleton
    public ListenerRegistration<FabricEventListener> listenerRegistration() {
        return new FabricEventRegistration();
    }

    /*
    DebugAddon / PlatformInjector
     */

    @Provides
    @Singleton
    public CommonPlatformInjector platformInjector() {
        return FabricInjector.getInstance();
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
    @Singleton
    public PluginMessageUtils pluginMessageUtils() {
        return new FabricPluginMessageUtils();
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Fabric";
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegister() {
        return new FabricPluginMessageRegistration();
    }

    @Provides
    @Singleton
    public SkinApplier skinApplier() {
        return new FabricSkinApplier();
    }
}
