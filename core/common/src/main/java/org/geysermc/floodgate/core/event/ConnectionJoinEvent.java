/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/Floodgate
 */
package org.geysermc.floodgate.core.event;

import net.kyori.adventure.text.Component;
import org.geysermc.api.connection.Connection;
import org.geysermc.event.util.AbstractCancellable;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.util.LinkedPlayer;

public class ConnectionJoinEvent extends AbstractCancellable {
    private FloodgateConnection connection;
    private Component disconnectReason;

    public ConnectionJoinEvent(FloodgateConnection connection, Component disconnectReason) {
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

    public Component disconnectReason() {
        return disconnectReason;
    }

    public ConnectionJoinEvent disconnectReason(Component disconnectReason) {
        this.disconnectReason = disconnectReason;
        return this;
    }

    public boolean shouldDisconnect() {
        return disconnectReason != null;
    }
}
