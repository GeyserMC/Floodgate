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

package com.minekube.connect.listener;

import com.destroystokyo.paper.event.profile.PreFillProfileEvent;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.inject.Inject;
import com.minekube.connect.api.SimpleConnectApi;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PaperProfileListener implements Listener {
    @Inject private SimpleConnectApi api;

    @EventHandler // TODO robin: remove or replace with session proposal player props
    public void onFill(PreFillProfileEvent event) {
        UUID id = event.getPlayerProfile().getId();
        // back when this event got added the PlayerProfile class didn't have the
        // hasProperty / hasTextures methods
        if (id == null || !this.api.isConnectPlayer(id) ||
                event.getPlayerProfile().getProperties().stream().anyMatch(
                        prop -> "textures".equals(prop.getName()))) {
            return;
        }

        Set<ProfileProperty> properties = new HashSet<>(event.getPlayerProfile().getProperties());
        properties.add(new ProfileProperty("textures", "", ""));
        event.setProperties(properties);
    }
}
