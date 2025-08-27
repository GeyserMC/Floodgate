package org.geysermc.floodgate.platform.fabric.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.geysermc.floodgate.core.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.mod.listener.ModEventListener;
import org.geysermc.floodgate.mod.module.ModPlatformModule;
import org.geysermc.floodgate.platform.fabric.listener.FabricEventRegistration;
import org.geysermc.floodgate.platform.fabric.pluginmessage.FabricPluginMessageRegistration;
import org.geysermc.floodgate.platform.fabric.pluginmessage.FabricPluginMessageUtils;

public class FabricPlatformModule extends ModPlatformModule {

    @Provides
    @Singleton
    public ListenerRegistration<ModEventListener> listenerRegistration() {
        return new FabricEventRegistration();
    }

    @Provides
    @Singleton
    public PluginMessageUtils pluginMessageUtils() {
        return new FabricPluginMessageUtils();
    }

    @Provides
    @Singleton
    public PluginMessageRegistration pluginMessageRegister() {
        return new FabricPluginMessageRegistration();
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "Fabric";
    }
}
