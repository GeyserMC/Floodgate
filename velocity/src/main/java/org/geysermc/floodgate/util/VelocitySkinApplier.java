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

package org.geysermc.floodgate.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinData;

@RequiredArgsConstructor
public class VelocitySkinApplier implements SkinApplier {
    private final ProxyServer server;

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, SkinData skinData) {
        server.getPlayer(floodgatePlayer.getCorrectUniqueId()).ifPresent(player -> {
            List<Property> properties = new ArrayList<>(player.getGameProfileProperties());
            properties.add(new Property("textures", skinData.getValue(), skinData.getSignature()));
            player.setGameProfileProperties(properties);
        });
    }

    @Override
    public boolean hasSkin(FloodgatePlayer floodgatePlayer) {
        Optional<Player> player = server.getPlayer(floodgatePlayer.getCorrectUniqueId());

        if (player.isPresent()) {
            for (Property property : player.get().getGameProfileProperties()) {
                if (property.getName().equals("textures")) {
                    if (!property.getValue().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
