package org.geysermc.floodgate.core.connection.integrated;

import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateConnection;

public interface IntegratedDataSeeker extends DataSeeker {
    void addConnection(Object identifier, FloodgateConnection connection);
}
