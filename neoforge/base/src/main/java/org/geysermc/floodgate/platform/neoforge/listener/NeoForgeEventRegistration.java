package org.geysermc.floodgate.platform.neoforge.listener;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.geysermc.floodgate.core.platform.listener.ListenerRegistration;
import org.geysermc.floodgate.mod.listener.ModEventListener;

public final class NeoForgeEventRegistration implements ListenerRegistration<ModEventListener> {
    private ModEventListener listener;

    @Override
    public void register(ModEventListener listener) {
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        this.listener = listener;
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        listener.onPlayerJoin(event.getEntity().getUUID());
    }
}
