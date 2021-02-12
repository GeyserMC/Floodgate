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
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

public final class SpigotSkinApplier implements SkinApplier {
    private static final Method GET_PROFILE_METHOD;

    static {
        String version = ReflectionUtils.getPrefix().split("\\.")[3];
        Class<?> craftPlayerClass = ReflectionUtils.getClass(
                "org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
        GET_PROFILE_METHOD = ReflectionUtils.getMethod(craftPlayerClass, "getProfile");
    }

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
        Player player = Bukkit.getPlayer(floodgatePlayer.getCorrectUniqueId());
        GameProfile profile = ReflectionUtils.castedInvoke(player, GET_PROFILE_METHOD);

        if (profile == null) {
            throw new IllegalStateException("The GameProfile cannot be null! " + player.getName());
        }

        PropertyMap properties = profile.getProperties();

        //todo check if removing all texture properties breaks some stuff
        properties.removeAll("textures");
        Property property = new Property(
                "textures",
                skinResult.get("value").getAsString(),
                skinResult.get("signature").getAsString());
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
