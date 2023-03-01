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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile.Property;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.skin.SkinApplyEventImpl;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinDataImpl;

@Singleton
public class VelocitySkinApplier implements SkinApplier {
    @Inject private ProxyServer server;
    @Inject private EventBus eventBus;

    @Override
    public void applySkin(@NonNull FloodgatePlayer floodgatePlayer, @NonNull SkinData skinData) {
        server.getPlayer(floodgatePlayer.getCorrectUniqueId()).ifPresent(player -> {
            List<Property> properties = new ArrayList<>(player.getGameProfileProperties());

            SkinData currentSkin = currentSkin(properties);

            SkinApplyEvent event = new SkinApplyEventImpl(floodgatePlayer, currentSkin, skinData);
            event.setCancelled(floodgatePlayer.isLinked());

            eventBus.fire(event);

            if (event.isCancelled()) {
                return;
            }

            replaceSkin(properties, event.newSkin());
            player.setGameProfileProperties(properties);
        });
    }

    private SkinData currentSkin(List<Property> properties) {
        for (Property property : properties) {
            if (property.getName().equals("textures")) {
                if (!property.getValue().isEmpty()) {
                    return new SkinDataImpl(property.getValue(), property.getSignature());
                }
            }
        }
        return null;
    }

    private void replaceSkin(List<Property> properties, SkinData skinData) {
        properties.removeIf(property -> property.getName().equals("textures"));
        properties.add(new Property("textures", skinData.value(), skinData.signature()));
    }
}
