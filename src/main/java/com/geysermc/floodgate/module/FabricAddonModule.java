package com.geysermc.floodgate.module;

import com.geysermc.floodgate.addon.data.FabricDataAddon;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.geysermc.floodgate.addon.AddonManagerAddon;
import org.geysermc.floodgate.addon.DebugAddon;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.register.AddonRegister;

public class FabricAddonModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AddonRegister.class).asEagerSingleton();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon managerAddon() {
        return new AddonManagerAddon();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon dataAddon() {
        return new FabricDataAddon();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon debugAddon() {
        return new DebugAddon();
    }
}
