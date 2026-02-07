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

package org.geysermc.floodgate.pluginmessage;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.ReflectionUtils.getFieldOfType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.skin.SkinApplyEventImpl;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinDataImpl;
import org.geysermc.floodgate.util.ReflectionUtils;

@Singleton
public final class BungeeSkinApplier implements SkinApplier {
    private static final Field LOGIN_RESULT_FIELD;

    static {
        LOGIN_RESULT_FIELD = getFieldOfType(InitialHandler.class, LoginResult.class);
        checkNotNull(LOGIN_RESULT_FIELD, "LoginResult field cannot be null");
    }

    private final ProxyServer server = ProxyServer.getInstance();

    @Inject private EventBus eventBus;
    @Inject private FloodgateLogger logger;

    @Override
    public void applySkin(@NonNull FloodgatePlayer floodgatePlayer, @NonNull SkinData skinData, boolean internal) {
        ProxiedPlayer player = server.getPlayer(floodgatePlayer.getCorrectUniqueId());
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
        // which Floodgate players don't have
        if (loginResult == null) {
            // id and name are unused
            loginResult = new LoginResult(null, null, new Property[0]);
            ReflectionUtils.setValue(handler, LOGIN_RESULT_FIELD, loginResult);
        }

        Property[] properties = loginResult.getProperties();

        SkinData currentSkin = currentSkin(properties);

        SkinApplyEvent event = new SkinApplyEventImpl(floodgatePlayer, currentSkin, skinData);
        event.setCancelled(!internal && floodgatePlayer.isLinked());

        eventBus.fire(event);

        if (event.isCancelled()) {
            return;
        }

        loginResult.setProperties(replaceSkin(properties, event.newSkin()));
    }

    private SkinData currentSkin(Property[] properties) {
        for (Property property : properties) {
            if (property.getName().equals("textures")) {
                if (!property.getValue().isEmpty()) {
                    return new SkinDataImpl(property.getValue(), property.getSignature());
                }
            }
        }
        return null;
    }

    private Property[] replaceSkin(Property[] properties, SkinData skinData) {
        List<Property> list = new ArrayList<>();
        for (Property property : properties) {
            if (!property.getName().equals("textures")) {
                list.add(property);
            }
        }
        list.add(new Property("textures", skinData.value(), skinData.signature()));
        return list.toArray(new Property[0]);
    }
}
