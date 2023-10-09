package org.geysermc.floodgate.core.event;

import org.geysermc.api.connection.Connection;
import org.geysermc.event.util.AbstractCancellable;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.util.LinkedPlayer;

public class ConnectionJoinEvent extends AbstractCancellable {
    private FloodgateConnection connection;
    private String disconnectReason;

    public ConnectionJoinEvent(FloodgateConnection connection, String disconnectReason) {
        this.connection = connection;
        this.disconnectReason = disconnectReason;
    }

    public Connection connection() {
        return connection;
    }

    public ConnectionJoinEvent linkedPlayer(LinkedPlayer linkedPlayer) {
        connection = connection.linkedPlayer(linkedPlayer);
        return this;
    }

    public String disconnectReason() {
        return disconnectReason;
    }

    public ConnectionJoinEvent disconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
        return this;
    }

    public boolean shouldDisconnect() {
        return disconnectReason != null;
    }
}
