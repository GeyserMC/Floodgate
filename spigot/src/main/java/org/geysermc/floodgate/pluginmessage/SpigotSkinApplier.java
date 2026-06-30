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
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent.SkinData;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.event.EventBus;
import org.geysermc.floodgate.event.skin.SkinApplyEventImpl;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.util.ClassNames;
import org.geysermc.floodgate.util.ReflectionUtils;
import org.geysermc.floodgate.util.SpigotVersionSpecificMethods;

@Singleton
public final class SpigotSkinApplier implements SkinApplier {
    @Inject private SpigotVersionSpecificMethods versionSpecificMethods;
    @Inject private EventBus eventBus;
    @Inject private FloodgateLogger logger;
    @Inject private FloodgateConfig config;

    @Override
    public void applySkin(@NonNull FloodgatePlayer floodgatePlayer, @NonNull SkinData skinData, boolean internal) {
        logSkinDebug("Starting skin apply for player={} internal={} valueLength={} signatureLength={}",
                floodgatePlayer.getCorrectUsername(), internal,
                skinData.value().length(), skinData.signature().length());
        applySkin0(floodgatePlayer, skinData, internal, true);
    }

    private void applySkin0(FloodgatePlayer floodgatePlayer, SkinData skinData, boolean internal, boolean firstTry) {
        Player player = Bukkit.getPlayer(floodgatePlayer.getCorrectUniqueId());

        // player is probably not logged in yet
        if (player == null) {
            if (firstTry) {
                logSkinDebug("Player {} not online yet; scheduling delayed skin apply retry",
                        floodgatePlayer.getCorrectUsername());
                versionSpecificMethods.schedule(
                        () -> applySkin0(floodgatePlayer, skinData, internal, false),
                        10 * 20
                );
            } else {
                logSkinDebug("Player {} still unavailable after delayed retry; skin apply aborted",
                        floodgatePlayer.getCorrectUsername());
            }
            return;
        }

        GameProfile profile = ReflectionUtils.castedInvoke(player, ClassNames.GET_PROFILE_METHOD);

        if (profile == null) {
            throw new IllegalStateException("The GameProfile cannot be null! " + player.getName());
        }

        // Need to be careful here - getProperties() returns an authlib PropertyMap, which extends
        // MultiMap from Guava. Floodgate relocates Guava.
        SkinData currentSkin = versionSpecificMethods.currentSkin(profile);
        logSkinDebug("Current skin for {} present={}", player.getName(), currentSkin != null);

        SkinApplyEvent event = new SkinApplyEventImpl(floodgatePlayer, currentSkin, skinData);
        event.setCancelled(!internal && floodgatePlayer.isLinked());
        logSkinDebug("Skin event initial cancelled={} (linked={} internal={})",
                event.isCancelled(), floodgatePlayer.isLinked(), internal);

        eventBus.fire(event);
        logSkinDebug("Skin event after listeners cancelled={} for {}",
                event.isCancelled(), player.getName());

        if (event.isCancelled()) {
            logSkinDebug("Skin apply cancelled for {} by event pipeline", player.getName());
            return;
        }

        if (ClassNames.GAME_PROFILE_FIELD != null) {
            logSkinDebug("Applying skin through immutable GameProfile path for {}", player.getName());
            replaceSkin(player, profile, event.newSkin());
        } else {
            // We're on a version with mutable GameProfiles
            logSkinDebug("Applying skin through mutable GameProfile path for {}", player.getName());
            replaceSkinOld(profile.getProperties(), event.newSkin());
        }

        versionSpecificMethods.maybeSchedule(() -> {
            int refreshedViewers = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player) && p.canSee(player)) {
                    versionSpecificMethods.hideAndShowPlayer(p, player);
                    refreshedViewers++;
                }
            }
            logSkinDebug("Completed hide/show refresh for {} viewers of {}", refreshedViewers,
                    player.getName());
        });
    }

    private void replaceSkin(Player player, GameProfile oldProfile, SkinData skinData) {
        Property skinProperty = new Property("textures", skinData.value(), skinData.signature());
        GameProfile profile = versionSpecificMethods.createGameProfile(oldProfile, skinProperty);
        Object entityHuman = ReflectionUtils.invoke(player, ClassNames.GET_ENTITY_HUMAN_METHOD);
        ReflectionUtils.setValue(entityHuman, ClassNames.GAME_PROFILE_FIELD, profile);
    }

    private void replaceSkinOld(PropertyMap properties, SkinData skinData) {
        properties.removeAll("textures");
        Property property = new Property("textures", skinData.value(), skinData.signature());
        properties.put("textures", property);
    }

    private void logSkinDebug(String message, Object... args) {
        if (config.isSkinUploadDebug()) {
            logger.info("[skin-upload-debug] " + message, args);
        }
    }
}
