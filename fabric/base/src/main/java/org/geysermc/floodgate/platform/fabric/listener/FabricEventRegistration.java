package org.geysermc.floodgate.platform.fabric.listener;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.geysermc.floodgate.core.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.mod.listener.ModEventListener;

public final class FabricEventRegistration implements ListenerRegistration<ModEventListener> {
    @Override
    public void register(ModEventListener listener) {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> listener.onPlayerJoin(handler.getPlayer().getUUID())
        );
    }
}
