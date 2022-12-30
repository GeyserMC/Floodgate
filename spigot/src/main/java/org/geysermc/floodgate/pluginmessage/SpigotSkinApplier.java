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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.skin.SkinApplyEventImpl;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinDataImpl;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

@Singleton
public final class SpigotSkinApplier implements SkinApplier {
    @Inject private SpigotVersionSpecificMethods versionSpecificMethods;
    @Inject private SpigotPlugin plugin;
    @Inject private EventBus eventBus;

    @Override
    public void applySkin(@NonNull Connection floodgatePlayer, @NonNull SkinData skinData) {
        applySkin0(floodgatePlayer, skinData, true);
    }

    private void applySkin0(Connection floodgatePlayer, SkinData skinData, boolean firstTry) {
        Player player = Bukkit.getPlayer(floodgatePlayer.javaUuid());

        // player is probably not logged in yet
        if (player == null) {
            if (firstTry) {
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> applySkin0(floodgatePlayer, skinData, false),
                        10 * 20
                );
            }
            return;
        }

        GameProfile profile = ReflectionUtils.castedInvoke(player, ClassNames.GET_PROFILE_METHOD);

        if (profile == null) {
            throw new IllegalStateException("The GameProfile cannot be null! " + player.getName());
        }

        // Need to be careful here - getProperties() returns an authlib PropertyMap, which extends
        // MultiMap from Guava. Floodgate relocates Guava.
        PropertyMap properties = profile.getProperties();

        SkinData currentSkin = currentSkin(properties);

        SkinApplyEvent event = new SkinApplyEventImpl(floodgatePlayer, currentSkin, skinData);
        event.setCancelled(floodgatePlayer.isLinked());

        eventBus.fire(event);

        if (event.isCancelled()) {
            return;
        }

        replaceSkin(properties, event.newSkin());

        // By running as a task, we don't run into async issues
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player) && p.canSee(player)) {
                    versionSpecificMethods.hidePlayer(p, player);
                    versionSpecificMethods.showPlayer(p, player);
                }
            }
        });
    }

    private SkinData currentSkin(PropertyMap properties) {
        for (Property texture : properties.get("textures")) {
            if (!texture.getValue().isEmpty()) {
                return new SkinDataImpl(texture.getValue(), texture.getSignature());
            }
        }
        return null;
    }

    private void replaceSkin(PropertyMap properties, SkinData skinData) {
        properties.removeAll("textures");
        Property property = new Property("textures", skinData.value(), skinData.signature());
        properties.put("textures", property);
    }
}
