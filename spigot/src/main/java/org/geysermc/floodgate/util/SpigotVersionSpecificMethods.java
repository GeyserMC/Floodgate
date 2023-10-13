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

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.SpigotPlugin;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.skin.SkinDataImpl;

public final class SpigotVersionSpecificMethods {
    private static final Method GET_SPIGOT;
    private static final Method OLD_GET_LOCALE;
    private static final boolean NEW_VISIBILITY;

    private static final Method NEW_PROPERTY_VALUE;
    private static final Method NEW_PROPERTY_SIGNATURE;

    static {
        GET_SPIGOT = ReflectionUtils.getMethod(Player.class, "spigot");
        OLD_GET_LOCALE = ReflectionUtils.getMethod(Player.Spigot.class, "getLocale");

        NEW_VISIBILITY = null != ReflectionUtils.getMethod(
                Player.class, "hidePlayer",
                Plugin.class, Player.class
        );

        NEW_PROPERTY_VALUE = ReflectionUtils.getMethod(Property.class, "value");
        NEW_PROPERTY_SIGNATURE = ReflectionUtils.getMethod(Property.class, "signature");
    }

    private final SpigotPlugin plugin;

    public SpigotVersionSpecificMethods(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    public String getLocale(Player player) {
        if (OLD_GET_LOCALE == null) {
            return player.getLocale();
        }
        Object spigot = ReflectionUtils.invoke(player, GET_SPIGOT);
        return ReflectionUtils.castedInvoke(spigot, OLD_GET_LOCALE);
    }

    public void hideAndShowPlayer(Player on, Player target) {
        // In Folia we don't have to schedule this as there is no concept of a single main thread.
        // Instead, we have to schedule the task per player.
        if (ClassNames.IS_FOLIA) {
            on.getScheduler().execute(plugin, () -> hideAndShowPlayer0(on, target), null, 0);
            return;
        }
        hideAndShowPlayer0(on, target);
    }

    public SkinApplyEvent.SkinData currentSkin(PropertyMap properties) {
        for (Property property : properties.get("textures")) {
            String value;
            String signature;
            if (NEW_PROPERTY_VALUE != null) {
                value = ReflectionUtils.castedInvoke(property, NEW_PROPERTY_VALUE);
                signature = ReflectionUtils.castedInvoke(property, NEW_PROPERTY_SIGNATURE);
            } else {
                value = property.getValue();
                signature = property.getSignature();
            }

            //noinspection DataFlowIssue
            if (!value.isEmpty()) {
                return new SkinDataImpl(value, signature);
            }
        }
        return null;
    }

    public void schedule(Runnable runnable, long delay) {
        if (ClassNames.IS_FOLIA) {
            plugin.getServer().getAsyncScheduler().runDelayed(
                    plugin, $ -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS
            );
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @SuppressWarnings("deprecation")
    private void hideAndShowPlayer0(Player source, Player target) {
        if (NEW_VISIBILITY) {
            source.hidePlayer(plugin, target);
            source.showPlayer(plugin, target);
            return;
        }
        source.hidePlayer(target);
        source.showPlayer(target);
    }

    public void maybeSchedule(Runnable runnable) {
        // In Folia we don't have to schedule this as there is no concept of a single main thread.
        // Instead, we have to schedule the task per player.
        if (ClassNames.IS_FOLIA) {
            runnable.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
}
