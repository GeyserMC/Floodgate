package org.geysermc.floodgate.core.crypto;

import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.connection.codec.FloodgateConnectionCodec;

@Singleton
public final class FloodgateDataCodec {
    private final FloodgateFormatCodec formatCodec;
    private final FloodgateConnectionCodec connectionCodec;

    public FloodgateDataCodec(FloodgateFormatCodec formatCodec, FloodgateConnectionCodec connectionCodec) {
        this.formatCodec = formatCodec;
        this.connectionCodec = connectionCodec;
    }

    public byte[] encode(FloodgateConnection connection) throws Exception {
        return formatCodec.encode(connectionCodec.encode(connection));
    }

    public String encodeToString(FloodgateConnection connection) throws Exception {
        return new String(encode(connection), StandardCharsets.UTF_8);
    }

    public FloodgateConnection decode(byte[] data) throws Exception {
        return connectionCodec.decode(formatCodec.decode(data));
    }
}
