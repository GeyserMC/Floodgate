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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private static final Method NEW_GAME_PROFILE_PROPERTIES;
    private static final Constructor<GameProfile> RECORD_GAME_PROFILE_CONSTRUCTOR;
    private static final Constructor<PropertyMap> IMMUTABLE_PROPERTY_MAP_CONSTRUCTOR;
    private static final Method MULTIMAP_FROM_MAP;
    private static final Field PROFILE_NAME_FIELD;
    private static final Field PROFILE_UUID_FIELD;

    static {
        GET_SPIGOT = ReflectionUtils.getMethod(Player.class, "spigot");
        OLD_GET_LOCALE = ReflectionUtils.getMethod(Player.Spigot.class, "getLocale");

        NEW_VISIBILITY = null != ReflectionUtils.getMethod(
                Player.class, "hidePlayer",
                Plugin.class, Player.class
        );

        NEW_PROPERTY_VALUE = ReflectionUtils.getMethod(Property.class, "value");
        NEW_PROPERTY_SIGNATURE = ReflectionUtils.getMethod(Property.class, "signature");
        NEW_GAME_PROFILE_PROPERTIES = ReflectionUtils.getMethod(
                GameProfile.class, "properties");
        RECORD_GAME_PROFILE_CONSTRUCTOR = ReflectionUtils.getConstructor(
                GameProfile.class, true, UUID.class, String.class, PropertyMap.class);
        IMMUTABLE_PROPERTY_MAP_CONSTRUCTOR = (Constructor<PropertyMap>)
                PropertyMap.class.getConstructors()[0];
        PROFILE_NAME_FIELD = ReflectionUtils.getField(GameProfile.class, "name");
        PROFILE_UUID_FIELD = ReflectionUtils.getField(GameProfile.class, "id");
        // Avoid relocation for this class.
        Class<?> multimaps = ReflectionUtils.getClass(String.join(".", "com",
                "google", "common", "collect", "Multimaps"));
        MULTIMAP_FROM_MAP = ReflectionUtils.getMethod(multimaps, "forMap", Map.class);
    }

    private final SpigotPlugin plugin;

    public SpigotVersionSpecificMethods(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    public GameProfile createGameProfile(GameProfile oldProfile, Property textureProperty) {
        String name = (String) ReflectionUtils.getValue(oldProfile, PROFILE_NAME_FIELD);
        UUID uuid = (UUID) ReflectionUtils.getValue(oldProfile, PROFILE_UUID_FIELD);
        return createGameProfile(uuid, name, textureProperty);
    }

    public GameProfile createGameProfile(UUID uuid, String name, Property texturesProperty) {
        if (RECORD_GAME_PROFILE_CONSTRUCTOR != null && IMMUTABLE_PROPERTY_MAP_CONSTRUCTOR != null) {
            if (texturesProperty != null) {
                Map<String, Property> properties = new HashMap<>();
                properties.put("textures", texturesProperty);
                Object multimap = ReflectionUtils.invoke(null, MULTIMAP_FROM_MAP, properties);
                return ReflectionUtils.newInstanceOrThrow(RECORD_GAME_PROFILE_CONSTRUCTOR, uuid,
                        name,
                        ReflectionUtils.newInstanceOrThrow(IMMUTABLE_PROPERTY_MAP_CONSTRUCTOR,
                                multimap));
            }
        }
        GameProfile profile = new GameProfile(uuid, name);
        if (texturesProperty != null) {
            profile.getProperties().put("textures", texturesProperty);
        }
        return profile;
    }

    public String getLocale(Player player) {
        if (OLD_GET_LOCALE == null) {
            return player.getLocale();
        }
        Object spigot = ReflectionUtils.invoke(player, GET_SPIGOT);
        return ReflectionUtils.castedInvoke(spigot, OLD_GET_LOCALE);
    }

    public void hideAndShowPlayer(Player on, Player target) {
        // In Folia, we don't have to schedule this as there is no concept of a single main thread.
        // Instead, we have to schedule the task per player.
        // We use separate schedulers for hide and show to avoid race conditions that can crash the
        // server on Folia only.
        if (ClassNames.IS_FOLIA) {
            on.getScheduler().run(plugin, task -> hideAndShowPlayerHide(on, target), null);
            on.getScheduler().run(plugin, task -> hideAndShowPlayerShow(on, target), null);
            return;
        }
        hideAndShowPlayerHide(on, target);
        hideAndShowPlayerShow(on, target);
    }

    public SkinApplyEvent.SkinData currentSkin(GameProfile profile) {
        PropertyMap properties;
        if (NEW_GAME_PROFILE_PROPERTIES != null) {
            properties = ReflectionUtils.castedInvoke(profile, NEW_GAME_PROFILE_PROPERTIES);
        } else {
            properties = profile.getProperties();
        }

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
    private void hideAndShowPlayerHide(Player source, Player target) {
        if (NEW_VISIBILITY) {
            source.hidePlayer(plugin, target);
            return;
        }
        source.hidePlayer(target);
    }

    @SuppressWarnings("deprecation")
    private void hideAndShowPlayerShow(Player source, Player target) {
        if (NEW_VISIBILITY) {
            source.showPlayer(plugin, target);
            return;
        }
        source.showPlayer(target);
    }

    public void maybeSchedule(Runnable runnable) {
        this.maybeSchedule(runnable, false);
    }

    public void maybeSchedule(Runnable runnable, boolean globalContext) {
        // In Folia, we don't usually have to schedule this as there is no concept of a single main thread.
        // Instead, we have to schedule the task per player.
        // However, in some cases we may want to access the global region for a global context.
        if (ClassNames.IS_FOLIA) {
            if (globalContext) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run());
            } else {
                runnable.run();
            }
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
}
