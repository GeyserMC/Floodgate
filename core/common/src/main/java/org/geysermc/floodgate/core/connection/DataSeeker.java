package org.geysermc.floodgate.core.connection;

import jakarta.annotation.Nullable;

public interface DataSeeker {
    /**
     * Tried to seek Floodgate data from the provided arguments.
     * This method can throw the exceptions listed in {@link FloodgateDataHandler#decodeDataToConnection(String)}
     *
     * @param hostname the hostname sent by the client
     * @param extraData extra data which can give extra context
     */
    DataSeekerResult seekData(String hostname, Object extraData) throws Exception;

    /**
     * @param connection not null when the data contained Floodgate data
     * @param dataRemainder the remaining data when the Floodgate data is removed
     */
    record DataSeekerResult(@Nullable FloodgateConnection connection, String dataRemainder) {}
}
