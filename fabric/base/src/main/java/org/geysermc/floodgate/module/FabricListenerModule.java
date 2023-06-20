package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.geysermc.floodgate.listener.FabricEventListener;
import org.geysermc.floodgate.register.ListenerRegister;

public final class FabricListenerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<ListenerRegister<FabricEventListener>>() {}).asEagerSingleton();
    }

    @Singleton
    @ProvidesIntoSet
    public FabricEventListener fabricEventListener() {
        return new FabricEventListener();
    }
}
