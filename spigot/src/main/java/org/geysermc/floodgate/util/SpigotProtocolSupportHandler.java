/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.util;

import io.netty.channel.Channel;
import java.lang.reflect.Method;
import java.util.UUID;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.handshake.HandshakeHandler;

public class SpigotProtocolSupportHandler implements HandshakeHandler {
    private static final Method getFromChannel;
    private static final Method getLoginProfile;

    private static final Method setName;
    private static final Method setOriginalName;
    private static final Method setUuid;
    private static final Method setOriginalUuid;

    private static final Method getNetworkManagerWrapper;
    private static final Method getPacketListener;
    private static final Method handleLoginStart;

    static {
        Class<?> connectionImpl =
                ReflectionUtils.getClass("protocolsupport.protocol.ConnectionImpl");

        getFromChannel = ReflectionUtils.getMethod(connectionImpl, "getFromChannel", Channel.class);
        getLoginProfile = ReflectionUtils.getMethod(connectionImpl, "getLoginProfile");

        Class<?> profile = ReflectionUtils.getClass("protocolsupport.api.utils.Profile");

        setName = ReflectionUtils.getMethod(profile, "setName", String.class);
        setOriginalName = ReflectionUtils.getMethod(profile, "setOriginalName", String.class);
        setUuid = ReflectionUtils.getMethod(profile, "setUUID", UUID.class);
        setOriginalUuid = ReflectionUtils.getMethod(profile, "setOriginalUUID", UUID.class);

        getNetworkManagerWrapper =
                ReflectionUtils.getMethod(connectionImpl, "getNetworkManagerWrapper");

        Class<?> networkManagerWrapper =
                ReflectionUtils.getClass("protocolsupport.zplatform.network.NetworkManagerWrapper");

        getPacketListener = ReflectionUtils.getMethod(networkManagerWrapper, "getPacketListener");

        Class<?> loginListener = ReflectionUtils.getClass(
                "protocolsupport.protocol.packet.handler.AbstractLoginListener");

        handleLoginStart = ReflectionUtils.getMethod(loginListener, "handleLoginStart", UUID.class);
    }

    @Override
    public void handle(HandshakeData data) {
        if (data.isFloodgatePlayer() && SpigotUtils.isBungeeData()) {
            Object connection = ReflectionUtils.invoke(null, getFromChannel, data.getChannel());
            Object profile = ReflectionUtils.invoke(connection, getLoginProfile);

            // set correct uuid and name on ProtocolSupport's end, since we skip the LoginStart
            ReflectionUtils.invoke(profile, setName, data.getCorrectUsername());
            ReflectionUtils.invoke(profile, setOriginalName, data.getCorrectUsername());
            ReflectionUtils.invoke(profile, setUuid, data.getCorrectUniqueId());
            ReflectionUtils.invoke(profile, setOriginalUuid, data.getCorrectUniqueId());

            Object temp = ReflectionUtils.invoke(connection, getNetworkManagerWrapper);
            temp = ReflectionUtils.invoke(temp, getPacketListener);
            ReflectionUtils.invoke(temp, handleLoginStart, data.getCorrectUsername());
        }
    }
}
