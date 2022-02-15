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

package com.minekube.connect.util;

import com.minekube.connect.SpigotPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SpigotVersionSpecificMethods {
    private static final boolean NEW_GET_LOCALE;
    private static final boolean NEW_VISIBILITY;

    static {
        NEW_GET_LOCALE = ReflectionUtils.getMethod(Player.class, "getLocale") != null;
        NEW_VISIBILITY = null != ReflectionUtils.getMethod(
                Player.class, "hidePlayer",
                Plugin.class, Player.class
        );
    }

    private final SpigotPlugin plugin;

    public SpigotVersionSpecificMethods(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    public String getLocale(Player player) {
        if (NEW_GET_LOCALE) {
            return player.getLocale();
        }
        return player.spigot().getLocale();
    }

    @SuppressWarnings("deprecation")
    public void hidePlayer(Player hideFor, Player playerToHide) {
        if (NEW_VISIBILITY) {
            hideFor.hidePlayer(plugin, playerToHide);
            return;
        }
        hideFor.hidePlayer(playerToHide);
    }

    @SuppressWarnings("deprecation")
    public void showPlayer(Player showFor, Player playerToShow) {
        if (NEW_VISIBILITY) {
            showFor.showPlayer(plugin, playerToShow);
            return;
        }
        showFor.showPlayer(playerToShow);
    }
}
