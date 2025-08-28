package org.geysermc.floodgate.mod.listener;


import jakarta.inject.Inject;
import org.geysermc.floodgate.core.connection.ConnectionManager;

import java.util.UUID;
import org.geysermc.floodgate.core.util.LanguageManager;

public final class ModEventListener {

    @Inject
    LanguageManager languageManager;

    @Inject
    ConnectionManager connectionManager;

    public void onPlayerJoin(UUID uuid) {
        // TODO this might be called late on fabric
        var connection = connectionManager.findPendingConnection(uuid);
        if (connection == null) {
            return;
        }

        languageManager.loadLocale(connection.languageCode());
        connectionManager.addAcceptedConnection(connection);
    }

    public void onPlayerQuit(UUID uuid) {
        connectionManager.removeConnection(uuid);
    }
}
