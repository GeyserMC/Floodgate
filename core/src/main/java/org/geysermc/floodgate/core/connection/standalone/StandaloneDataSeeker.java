package org.geysermc.floodgate.core.connection.standalone;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.connection.DataSeeker;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.connection.FloodgateDataHandler;
import org.geysermc.floodgate.core.crypto.FloodgateFormatCodec;

@Singleton
public final class StandaloneDataSeeker implements DataSeeker {
    @Inject FloodgateDataHandler dataHandler;

    @Override
    public DataSeekerResult seekData(String hostname, Object ignored) {
        String[] hostnameItems = hostname.split("\0");
        String floodgateData = null;

        StringBuilder builder = new StringBuilder();
        for (String value : hostnameItems) {
            int version = FloodgateFormatCodec.version(value);
            if (floodgateData == null && version != -1) {
                floodgateData = value;
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\0');
            }
            builder.append(value);
        }

        FloodgateConnection connection = null;
        if (floodgateData != null) {
            connection = dataHandler.decodeDataToConnection(floodgateData);
        }

        // the new hostname doesn't have Floodgate data anymore, if it had Floodgate data.
        return new DataSeekerResult(connection, builder.toString());
    }
}
