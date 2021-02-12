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

package org.geysermc.floodgate.pluginmessage;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;

@RequiredArgsConstructor
public final class BungeeSkinApplier implements SkinApplier {
    private final FloodgateLogger logger;

    @Override
    public void applySkin(FloodgatePlayer uuid, JsonObject skinResult) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid.getCorrectUniqueId());

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
            // id and name are unused and properties will be overridden
            loginResult = new LoginResult(null, null, null);
        }

        Property property = new Property(
                "textures",
                skinResult.get("value").getAsString(),
                skinResult.get("signature").getAsString()
        );

        loginResult.setProperties(new Property[]{property});
    }
}
