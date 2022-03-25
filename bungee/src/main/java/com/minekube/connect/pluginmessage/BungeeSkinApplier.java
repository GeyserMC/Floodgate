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

import static com.minekube.connect.util.ReflectionUtils.getFieldOfType;
import static com.minekube.connect.util.ReflectionUtils.setValue;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.api.player.ConnectPlayer;
import com.minekube.connect.skin.SkinApplier;
import com.minekube.connect.skin.SkinData;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;

@RequiredArgsConstructor
public final class BungeeSkinApplier implements SkinApplier {
    private static final Field LOGIN_RESULT;

    static {
        LOGIN_RESULT = getFieldOfType(InitialHandler.class, LoginResult.class);
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
            logger.error("Incompatible BungeeCord fork detected", exception);
            return;
        }

        LoginResult loginResult = handler.getLoginProfile();
        // expected to be null since LoginResult is the data from hasJoined,
        // which Connect players don't have
        if (loginResult == null) {
            // id and name are unused and properties will be overridden
            loginResult = new LoginResult(null, null, null);
            setValue(handler, LOGIN_RESULT, loginResult);
        }

        Property property = new Property("textures", skinData.getValue(), skinData.getSignature());

        loginResult.setProperties(new Property[]{property});
    }
}
