package org.geysermc.floodgate.platform.neoforge.module;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.geysermc.floodgate.core.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.core.platform.pluginmessage.PluginMessageUtils;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageRegistration;
import org.geysermc.floodgate.mod.listener.ModEventListener;
import org.geysermc.floodgate.mod.module.ModPlatformModule;
import org.geysermc.floodgate.platform.neoforge.listener.NeoForgeEventRegistration;
import org.geysermc.floodgate.platform.neoforge.pluginmessage.NeoForgePluginMessageRegistration;
import org.geysermc.floodgate.platform.neoforge.pluginmessage.NeoForgePluginMessageUtils;

public class NeoForgePlatformModule extends ModPlatformModule {

    @Override
    protected void configure() {
        super.configure();

        // We retrieve using NeoForgePluginMessageRegistration.class from our the mod class.
        // We do this to ensure that injector#getInstance with either class returns the same singleton
        bind(PluginMessageRegistration.class).to(NeoForgePluginMessageRegistration.class).in(Scopes.SINGLETON);
        bind(NeoForgePluginMessageRegistration.class).toInstance(new NeoForgePluginMessageRegistration());
    }

    @Provides
    @Singleton
    public ListenerRegistration<ModEventListener> listenerRegistration() {
        return new NeoForgeEventRegistration();
    }

    @Provides
    @Singleton
    public PluginMessageUtils pluginMessageUtils() {
        return new NeoForgePluginMessageUtils();
    }

    @Provides
    @Named("implementationName")
    public String implementationName() {
        return "NeoForge";
    }

}
