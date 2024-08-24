package org.geysermc.floodgate.core.connection.integrated;

import io.micronaut.context.annotation.Replaces;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.connection.standalone.StandaloneDataSeeker;
import org.geysermc.floodgate.core.scope.IntegratedOnly;

@IntegratedOnly
@Replaces(StandaloneDataSeeker.class)
@Singleton
public class IntegratedNettyDataSeeker implements IntegratedDataSeeker {
    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Override
    public DataSeekerResult seekData(String hostname, Object extraData) {
        Channel channel = (Channel) extraData;
        var connection = (FloodgateConnection) channel.attr(connectionAttribute).get();
        return new DataSeekerResult(connection, hostname);
    }

    @Override
    public void addConnection(Object identifier, FloodgateConnection connection) {
        Channel channel = (Channel) identifier;
        channel.attr(connectionAttribute).set(connection);
    }
}
