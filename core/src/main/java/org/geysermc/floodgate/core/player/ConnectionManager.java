/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.core.player;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.logger.FloodgateLogger;

public abstract class ConnectionManager {
    private final Set<Connection> connections = Collections.synchronizedSet(new HashSet<>());
    private final Set<Connection> pendingConnections = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, Connection> xuidToConnection =
            Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Connection> uuidToConnection =
            Collections.synchronizedMap(new HashMap<>());
    protected final Map<Object, Connection> platformIdentifierToConnection =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject FloodgateLogger logger;

    public Connection connectionByUuid(UUID javaId) {
        return uuidToConnection.get(javaId);
    }

    public Connection connectionByXuid(String xuid) {
        return xuidToConnection.get(xuid);
    }

    public @Nullable Connection connectionByPlatformIdentifier(@NonNull Object platformIdentifier) {
        Objects.requireNonNull(platformIdentifier);
        var connection = platformIdentifierToConnection.get(platformIdentifier);
        if (connection != null) {
            return connection;
        }
        // try to fetch a connection or return a different platform identifier we can try
        var identifierOrConnection = platformIdentifierOrConnectionFor(platformIdentifier);
        if (identifierOrConnection == null) {
            return null;
        }
        // if it returns a connection it found a way to fetch it from the given platformIdentifier
        if (identifierOrConnection instanceof Connection foundConnection) {
            platformIdentifierToConnection.put(platformIdentifier, foundConnection);
            return foundConnection;
        }
        // if it returns a different platform identifier,
        // call this method again with the new identifier and store the result if it returned any
        connection = connectionByPlatformIdentifier(identifierOrConnection);
        if (connection != null) {
            platformIdentifierToConnection.put(identifierOrConnection, connection);
        }
        return connection;
    }

    protected abstract @Nullable Object platformIdentifierOrConnectionFor(Object input);

    public void addConnection(Connection connection) {
        connections.add(connection);
        pendingConnections.add(connection);

        logger.translatedInfo(
                "floodgate.ingame.login_name",
                connection.javaUsername(), connection.javaUuid()
        );
    }

    public boolean addAcceptedConnection(Connection connection) {
        pendingConnections.remove(connection);

        xuidToConnection.put(connection.xuid(), connection);
        var old = uuidToConnection.put(connection.javaUuid(), connection);
        if (old == null) {
            return false;
        }

        logger.debug(String.format(
                "Replaced Floodgate player playing as %s uuid %s with %s uuid %s",
                old.javaUsername(), old.javaUuid(),
                connection.javaUsername(), connection.javaUuid()
        ));
        return true;
    }

    public Connection findPendingConnection(UUID javaId) {
        for (Connection pendingConnection : pendingConnections) {
            if (pendingConnection.javaUuid().equals(javaId)) {
                return pendingConnection;
            }
        }
        return null;
    }

    public void removeConnection(Object platformIdentifier) {
        var connection = connectionByPlatformIdentifier(platformIdentifier);
        if (connection == null) {
            return;
        }

        connections.remove(connection);
        pendingConnections.remove(connection);
        uuidToConnection.remove(connection.javaUuid(), connection);
        xuidToConnection.remove(connection.xuid(), connection);
        logger.translatedInfo("floodgate.ingame.disconnect_name", connection.javaUsername());
    }

    public Collection<Connection> acceptedConnections() {
        return uuidToConnection.values();
    }

    public int acceptedConnectionsCount() {
        return uuidToConnection.size();
    }
}
