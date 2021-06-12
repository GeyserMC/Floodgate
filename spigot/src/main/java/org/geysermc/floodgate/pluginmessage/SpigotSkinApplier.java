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
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

public final class SpigotSkinApplier implements SkinApplier {
    private final SpigotVersionSpecificMethods versionSpecificMethods;
    private final SpigotPlugin plugin;

    public SpigotSkinApplier(
            SpigotVersionSpecificMethods versionSpecificMethods,
            SpigotPlugin plugin) {
        this.versionSpecificMethods = versionSpecificMethods;
        this.plugin = plugin;
    }

    @Override
    public void applySkin(FloodgatePlayer floodgatePlayer, JsonObject skinResult) {
        applySkin0(floodgatePlayer, skinResult, true);
    }

    private void applySkin0(FloodgatePlayer floodgatePlayer, JsonObject result, boolean firstTry) {
        Player player = Bukkit.getPlayer(floodgatePlayer.getCorrectUniqueId());

        // player is probably not logged in yet
        if (player == null) {
            if (firstTry) {
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> applySkin0(floodgatePlayer, result, false),
                        10 * 1000);
            }
            return;
        }

        GameProfile profile = ReflectionUtils.castedInvoke(player, ClassNames.GET_PROFILE_METHOD);

        if (profile == null) {
            throw new IllegalStateException("The GameProfile cannot be null! " + player.getName());
        }

        PropertyMap properties = profile.getProperties();

        properties.removeAll("textures");
        Property property = new Property(
                "textures",
                result.get("value").getAsString(),
                result.get("signature").getAsString());
        properties.put("textures", property);

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
}
