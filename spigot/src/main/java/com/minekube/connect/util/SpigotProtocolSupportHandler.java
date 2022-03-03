/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package com.minekube.connect.util;

import com.google.inject.Inject;
import com.minekube.connect.api.packet.PacketHandler;
import com.minekube.connect.api.packet.PacketHandlers;
import com.minekube.connect.network.netty.LocalSession;
import com.minekube.connect.network.netty.LocalSession.Context;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Method;
import java.util.UUID;

public class SpigotProtocolSupportHandler implements PacketHandler {
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

        Class<?> profile =
                ReflectionUtils.getClass("protocolsupport.protocol.utils.authlib.LoginProfile");

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

        handleLoginStart =
                ReflectionUtils.getMethod(loginListener, "handleLoginStart", String.class);
    }

    @Inject
    public void register(PacketHandlers packetHandlers) {
        packetHandlers.register(this, ClassNames.LOGIN_START_PACKET);
    }

    @Override
    public Object handle(ChannelHandlerContext ctx, Object packet, boolean serverbound) {
        LocalSession.context(ctx.channel()).map(Context::getPlayer).ifPresent(player -> {
            Object connection = ReflectionUtils.invoke(null, getFromChannel, ctx.channel());
            Object profile = ReflectionUtils.invoke(connection, getLoginProfile);

            // set correct uuid and name on ProtocolSupport's end, since we skip the LoginStart
            ReflectionUtils.invoke(profile, setName, player.getUsername());
            ReflectionUtils.invoke(profile, setOriginalName, player.getUsername());
            ReflectionUtils.invoke(profile, setUuid, player.getUniqueId());
            ReflectionUtils.invoke(profile, setOriginalUuid, player.getUniqueId());

            Object temp = ReflectionUtils.invoke(connection, getNetworkManagerWrapper);
            temp = ReflectionUtils.invoke(temp, getPacketListener);
            ReflectionUtils.invoke(temp, handleLoginStart, player.getUsername());
        });

        return packet;
    }
}
