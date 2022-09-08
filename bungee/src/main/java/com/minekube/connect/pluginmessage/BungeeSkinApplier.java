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

package com.minekube.connect.pluginmessage;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minekube.connect.util.ReflectionUtils.getConstructor;
import static com.minekube.connect.util.ReflectionUtils.getFieldOfType;
import static com.minekube.connect.util.ReflectionUtils.getMethodByName;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.player.ConnectPlayer;
import com.minekube.connect.skin.SkinApplier;
import com.minekube.connect.skin.SkinData;
import com.minekube.connect.util.ReflectionUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;

@RequiredArgsConstructor
public final class BungeeSkinApplier implements SkinApplier {
    private static final Constructor<?> LOGIN_RESULT_CONSTRUCTOR;
    private static final Field LOGIN_RESULT_FIELD;
    private static final Method SET_PROPERTIES_METHOD;

    private static final Class<?> PROPERTY_CLASS;
    private static final Constructor<?> PROPERTY_CONSTRUCTOR;


    static {
        PROPERTY_CLASS = ReflectionUtils.getClassOrFallbackPrefixed(
                "protocol.Property", "connection.LoginResult$Property"
        );

        LOGIN_RESULT_CONSTRUCTOR = getConstructor(
                LoginResult.class, true,
                String.class, String.class, Array.newInstance(PROPERTY_CLASS, 0).getClass()
        );

        LOGIN_RESULT_FIELD = getFieldOfType(InitialHandler.class, LoginResult.class);
        checkNotNull(LOGIN_RESULT_FIELD, "LoginResult field cannot be null");

        SET_PROPERTIES_METHOD = getMethodByName(LoginResult.class, "setProperties", true);

        PROPERTY_CONSTRUCTOR = ReflectionUtils.getConstructor(
                PROPERTY_CLASS, true,
                String.class, String.class, String.class
        );
        checkNotNull(PROPERTY_CONSTRUCTOR, "Property constructor cannot be null");
    }

    private final ConnectLogger logger;

    @Override
    public void applySkin(ConnectPlayer uuid, SkinData skinData) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid.getUniqueId());
        if (player == null) {
            return;
        }

        InitialHandler handler;
        try {
            handler = (InitialHandler) player.getPendingConnection();
        } catch (Exception exception) {
            logger.error("Incompatible Bungeecord fork detected", exception);
            return;
        }

        LoginResult loginResult = handler.getLoginProfile();
        // expected to be null since LoginResult is the data from hasJoined,
        // which Connect players don't have
        if (loginResult == null) {
            // id and name are unused and properties will be overridden
            loginResult = (LoginResult) ReflectionUtils.newInstance(
                    LOGIN_RESULT_CONSTRUCTOR, null, null, null
            );
            ReflectionUtils.setValue(handler, LOGIN_RESULT_FIELD, loginResult);
        }

        Object property = ReflectionUtils.newInstance(
                PROPERTY_CONSTRUCTOR,
                "textures", skinData.getValue(), skinData.getSignature()
        );

        Object propertyArray = Array.newInstance(PROPERTY_CLASS, 1);
        Array.set(propertyArray, 0, property);

        ReflectionUtils.invoke(loginResult, SET_PROPERTIES_METHOD, propertyArray);
    }
}
