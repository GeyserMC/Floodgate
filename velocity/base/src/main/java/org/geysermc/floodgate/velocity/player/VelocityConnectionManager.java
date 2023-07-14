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
import static org.geysermc.floodgate.core.util.ReflectionUtils.getFieldOfType;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getMethod;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getPrefixedClass;
import static org.geysermc.floodgate.core.util.ReflectionUtils.getValue;
import static org.geysermc.floodgate.core.util.ReflectionUtils.invoke;

import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.core.player.ConnectionManager;

@Singleton
public class VelocityConnectionManager extends ConnectionManager {
    private static final Class<?> LOGIN_INBOUND_CONNECTION;
    private static final Field INITIAL_CONNECTION_DELEGATE;
    private static final Class<?> INITIAL_INBOUND_CONNECTION;
    private static final Class<?> MINECRAFT_CONNECTION;
    private static final Method GET_CONNECTION;
    private static final Field CHANNEL;

    @Inject
    @Named("playerAttribute")
    AttributeKey<Connection> playerAttribute;

    @Override
    protected @Nullable Object platformIdentifierOrConnectionFor(Object input) {
        if (input instanceof Player) {
            // ConnectedPlayer implements VelocityInboundConnection,
            // just like InitialInboundConnection
            return invoke(input, GET_CONNECTION);
        }

        // LoginInboundConnection doesn't have a direct Channel reference,
        // but it does have an InitialInboundConnection reference
        if (LOGIN_INBOUND_CONNECTION.isInstance(input)) {
            return getValue(input, INITIAL_CONNECTION_DELEGATE);
        }

        // InitialInboundConnection -> MinecraftConnection -> Channel -> FloodgateConnection attribute

        if (INITIAL_INBOUND_CONNECTION.isInstance(input)) {
            return invoke(input, GET_CONNECTION);
        }
        if (MINECRAFT_CONNECTION.isInstance(input)) {
            return getValue(input, CHANNEL);
        }
        if (input instanceof Channel channel) {
            return channel.attr(playerAttribute).get();
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
        LOGIN_INBOUND_CONNECTION = getPrefixedClass("connection.client.LoginInboundConnection");
        INITIAL_CONNECTION_DELEGATE = getField(LOGIN_INBOUND_CONNECTION, "delegate");

        INITIAL_INBOUND_CONNECTION = getPrefixedClass("connection.client.InitialInboundConnection");
        MINECRAFT_CONNECTION = getPrefixedClass("connection.MinecraftConnection");

        Class<?> velocityInboundConnection = getPrefixedClass("connection.util.VelocityInboundConnection");
        GET_CONNECTION = getMethod(velocityInboundConnection, "getConnection");

        CHANNEL = getFieldOfType(MINECRAFT_CONNECTION, Channel.class);
    }
}
