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

import static com.minekube.connect.util.ReflectionUtils.getMethod;
import static com.minekube.connect.util.ReflectionUtils.getPrefixedClass;

import com.minekube.connect.api.ConnectApi;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("unchecked")
public final class SpigotProtocolSupportListener {
    public static void registerHack(Plugin plugin) {
        String prefix = ReflectionUtils.getPrefix();
        ReflectionUtils.setPrefix("protocolsupport.api");

        Class<? extends Event> playerLoginStartEvent =
                (Class<? extends Event>) getPrefixedClass("events.PlayerLoginStartEvent");

        Method setOnlineMode = getMethod(playerLoginStartEvent, "setOnlineMode", boolean.class);

        Class<?> connectionEvent = getPrefixedClass("events.ConnectionEvent");
        Method getConnection = getMethod(connectionEvent, "getConnection");

        Class<?> connection = getPrefixedClass("Connection");
        Method getProfile = getMethod(connection, "getProfile");

        Class<?> profile = getPrefixedClass("utils.Profile");
        Method getUuid = getMethod(profile, "getUUID");

        plugin.getServer().getPluginManager().registerEvent(
                playerLoginStartEvent, new Listener() {}, EventPriority.MONITOR,
                (listener, event) -> {
                    Object temp = ReflectionUtils.invoke(event, getConnection);
                    temp = ReflectionUtils.invoke(temp, getProfile);

                    UUID uuid = ReflectionUtils.castedInvoke(temp, getUuid);

                    // a normal player that doesn't have a UUID set
                    if (uuid == null) {
                        return;
                    }

                    if (ConnectApi.getInstance().isConnectPlayer(uuid)) {
                        // otherwise ProtocolSupport attempts to connect with online mode
                        ReflectionUtils.invoke(event, setOnlineMode, false);
                    }
                },
                plugin);

        ReflectionUtils.setPrefix(prefix);
    }
}
