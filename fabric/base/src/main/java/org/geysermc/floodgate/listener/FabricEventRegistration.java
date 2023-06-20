package org.geysermc.floodgate.listener;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.geysermc.floodgate.platform.listener.ListenerRegistration;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public final class FabricEventRegistration implements ListenerRegistration<FabricEventListener> {
    @Override
    public void register(FabricEventListener listener) {
        ServerPlayConnectionEvents.JOIN.register(listener::onPlayerJoin);
    }
}
