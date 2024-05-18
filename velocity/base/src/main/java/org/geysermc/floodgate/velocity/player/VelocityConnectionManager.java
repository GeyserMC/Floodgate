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

package org.geysermc.floodgate.velocity.player;

import static org.geysermc.floodgate.core.util.ReflectionUtils.getField;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getValue;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.lang.reflect.Field;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.connection.ConnectionManager;

@Singleton
public class VelocityConnectionManager extends ConnectionManager {
    private static final Field INITIAL_CONNECTION_DELEGATE;

    @Inject
    @Named("connectionAttribute")
    AttributeKey<Connection> connectionAttribute;

    @Override
    protected @Nullable Object platformIdentifierOrConnectionFor(Object input) {
        // ConnectedPlayer (Player) implements VelocityInboundConnection,
        // just like InitialInboundConnection
        if (input instanceof VelocityInboundConnection connection) {
            return connection.getConnection();
        }

        // LoginInboundConnection doesn't have a direct Channel reference,
        // but it does have an InitialInboundConnection reference
        if (input instanceof LoginInboundConnection) {
            return getValue(input, INITIAL_CONNECTION_DELEGATE);
        }

        // InitialInboundConnection -> MinecraftConnection -> Channel -> FloodgateConnection attribute
        if (input instanceof MinecraftConnection connection) {
            return connection.getChannel();
        }
        if (input instanceof Channel channel) {
            return channel.attr(connectionAttribute).get();
        }
        return null;
    }

    public Channel channelFor(Object input) {
        var result = platformIdentifierOrConnectionFor(input);
        if (result instanceof Channel channel) {
            return channel;
        }
        return channelFor(result);
    }

    static {
        INITIAL_CONNECTION_DELEGATE = getField(LoginInboundConnection.class, "delegate");
    }
}