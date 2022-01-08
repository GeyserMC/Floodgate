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

package org.geysermc.floodgate.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geysermc.floodgate.util.ReflectionUtils.getField;

import java.lang.reflect.Field;

@SuppressWarnings("ConstantConditions")
public final class ProxyUtils {
    private static final Field IS_BUNGEE_DATA;
    private static final Field IS_MODERN_FORWARDING;
    private static final boolean IS_PAPER_SERVER;

    static {
        Class<?> spigotConfig = ReflectionUtils.getClass("org.spigotmc.SpigotConfig");
        IS_BUNGEE_DATA = getField(spigotConfig, "bungee");
        checkNotNull(IS_BUNGEE_DATA, "bungee field cannot be null. Are you using CraftBukkit?");

        Field velocitySupport;
        boolean isPaper = false;

        try {
            Class<?> paperConfig = Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
            velocitySupport = getField(paperConfig, "velocitySupport");
        } catch (ClassNotFoundException e) {
            // We're not on a platform that has modern forwarding
            velocitySupport = null; // NOPMD - there's really not a better way around this unless you want to use an optional
        }
        IS_MODERN_FORWARDING = velocitySupport;
        IS_PAPER_SERVER = isPaper;
    }

    public static boolean isPaperServer() {
        return IS_PAPER_SERVER;
    }

    public static boolean isProxyData() {
        return isBungeeData() || isVelocitySupport();
    }

    private static boolean isBungeeData() {
        return ReflectionUtils.getCastedValue(null, IS_BUNGEE_DATA);
    }

    private static boolean isVelocitySupport() {
        if (IS_MODERN_FORWARDING == null) {
            return false;
        }

        return ReflectionUtils.getCastedValue(null, IS_MODERN_FORWARDING);
    }
}
